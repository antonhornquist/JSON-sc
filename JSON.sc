JSON {
	classvar
		indentString = "\t";

	*parse { |source, symbolizeNames=false|
		^JSONParser.new(source).parse(symbolizeNames)
	}

	*writeAsJSON { |obj, pathname, prettyPrint=false|
		var file;
		file = File.open(pathname,"w");
		file.write(obj.asJSON(prettyPrint));
		file.close;
	}

	*getIndentation { |levels|
		^(indentString ! levels).join
	}
}


/*
	The JSON Parser is based upon pure Ruby parser code of the JSON library at http://github.com/flori/json
*/
JSONParser : StringScanner {
	var
		rgObjectOpen = "\\{",
		rgObjectClose = "\\}",
		rgArrayOpen = "\\[",
		rgArrayClose = "\\]",
		rgPairDelimiter = ":",
		rgCollectionDelimiter = ",",
		rgIgnore = "\\s+",
		rgInteger = "(-?0|-?[1-9]\\d*)",
		rgFloat = "(-?(?:0|[1-9]\\d*)(?:\\.\\d+(?i:e[+-]?\\d+)|\\.\\d+|(?i:e[+-]?\\d+)))",
		rgString = "\"[0-9a-zA-Z_ @=*/+-:;,.()?&\\\\'']*\"",
		rgTrue = "true",
		rgFalse = "false",
		rgNull = "null",
		peekLength = 100,
		debugJSON = nil // nil = no debug, notNil = debug
	;

	*new { |source|
		^super.new(source).initJSON
	}

	initJSON { }

	parse { |symbolizeNames=false|
		var obj, caseResult;
		debugJSON !? { "> begin %".format(thisMethod.name).debug };
		this.reset;
		obj = nil;
		while { this.eos.not } {
			case
				{ this.scan(rgObjectOpen).notNil } {
					obj.notNil.if { Error("source '%' not in JSON".format(this.peek(peekLength))).throw };
					obj = this.parseObject(symbolizeNames)
				}
				{ this.scan(rgArrayOpen).notNil } {
					obj.notNil.if { Error("source '%' not in JSON".format(this.peek(peekLength))).throw };
					obj = this.parseArray(symbolizeNames)
				}
				{ this.skip(rgIgnore).notNil } { \ignored }
					.isNil.if { Error("source '%' not in JSON".format(this.peek(peekLength))).throw };
		};
		obj.isNil.if { Error("source did not contain any JSON!").throw };
		debugJSON !? { "< end %: %".format(thisMethod.name, obj).debug };
		^obj
	}

	parseObject { |symbolizeNames=false|
		var result, string, value, delim, breakWhileLoop;
		debugJSON !? { "> begin %".format(thisMethod.name).debug };
		result = if (symbolizeNames, IdentityDictionary, Dictionary).new;
		delim = false;
		breakWhileLoop = false;

		while { this.eos.not and: breakWhileLoop.not } {
			case
				{ (string = this.parseString) != \unparsed } {
					this.skip(rgIgnore);
					this.skip(rgPairDelimiter).isNil.if {
						Error("expected ':' in object at '%'!".format(this.peek(peekLength))).throw
					};
					this.skip(rgIgnore);
					if ((value = this.parseValue(symbolizeNames)) != \unparsed) {
						result.put(if (symbolizeNames, string.asSymbol, string), value);
						delim = false;
						this.skip(rgIgnore);
						if (this.scan(rgCollectionDelimiter).notNil) {
							delim = true;
						} {
							this.matches(rgObjectClose).notNil.if {
								\objectHasBeenClosed // not used anywhere, included for clarity
							} {
								Error("expected ',' or '}' in object at '%'!".format(this.peek(peekLength))).throw
							}
						}
					} {
						Error("expected value in object at '%'!".format(this.peek(peekLength))).throw
					}
				}
				{ this.scan(rgObjectClose).notNil } {
					delim.if { Error("expected next name, value pair in object at '%'!".format(this.peek(peekLength))).throw };
					breakWhileLoop = true;
				}
				{ this.skip(rgIgnore).notNil } { \ignored }
					.isNil.if { Error("unexpected token in object at '%'".format(this.peek(peekLength))).throw };
		};

		debugJSON !? { "< end %: %".format(thisMethod.name, result).debug };
		^result
	}

	parseArray { |symbolizeNames=false|
		var result, delim, value, breakWhileLoop;

		debugJSON !? { "> begin %".format(thisMethod.name).debug };

		result = Array.new;
		delim = false;
		breakWhileLoop = false;

		while { this.eos.not and: breakWhileLoop.not } {
			case
				{ (value = this.parseValue(symbolizeNames)) != \unparsed } {
					delim = false;
					result = result.add(value);
					this.skip(rgIgnore);
					if (this.scan(rgCollectionDelimiter).notNil) {
						delim = true
					} {
						this.matches(rgArrayClose).notNil.if {
							\arrayHasBeenClosed // not used anywhere, included for clarity
						} {
							Error("expected ',' or ']' in array at '%'!".format(this.peek(peekLength))).throw
						}
					}
				}
				{ this.scan(rgArrayClose).notNil } {
					delim.if { Error("expected next element in array at '%'!".format(this.peek(peekLength))).throw };
					breakWhileLoop = true;
				}
				{ this.skip(rgIgnore).notNil } { \ignored }
					.isNil.if { Error("unexpected token in array at '%'".format(this.peek(peekLength))).throw };
		};
		debugJSON !? { "< end %: %".format(thisMethod.name, result).debug };
		^result
	}

	parseValue { |symbolizeNames=false|
		var scanned, parsedString, result;

		debugJSON !? { "> begin %".format(thisMethod.name).debug };

		debugJSON !? { "| pos %: %".format(thisMethod.name, pos).debug };

		result = case
			{ (scanned = this.scan(rgFloat)).notNil } {
				debugJSON !? { "| %: got float".format(thisMethod.name).debug };
				scanned.asFloat
			}
			{ (scanned = this.scan(rgInteger)).notNil } {
				debugJSON !? { "| %: got integer".format(thisMethod.name).debug };
				scanned.asInteger
			}
			{ this.scan(rgTrue).notNil } {
				debugJSON !? { "| %: got true".format(thisMethod.name).debug };
				true
			}
			{ this.scan(rgFalse).notNil } {
				debugJSON !? { "| %: got false".format(thisMethod.name).debug };
				false
			}
			{ this.scan(rgNull).notNil } {
				debugJSON !? { "| %: got null".format(thisMethod.name).debug };
				nil
			}
			{ (parsedString = this.parseString) != \unparsed } {
				debugJSON !? { "| %: got string".format(thisMethod.name).debug };
				parsedString
			}
			{ this.scan(rgArrayOpen).notNil } {
				debugJSON !? { "| %: got array".format(thisMethod.name).debug };
				this.parseArray(symbolizeNames)
			}
			{ this.scan(rgObjectOpen).notNil } {
				debugJSON !? { "| %: got object".format(thisMethod.name).debug };
				this.parseObject(symbolizeNames)
			}
			{ true } {
				debugJSON !? { "| %: didn't get anything".format(thisMethod.name).debug };
				\unparsed
			};

		debugJSON !? { "< end %: %".format(thisMethod.name, result).debug };
		^result
	}

	parseString {
		var scanned, result;

		debugJSON !? { "> begin %".format(thisMethod.name).debug };

		result = if ((scanned = this.scan(rgString)).notNil) {
			if (scanned.size == 2) {
				""
			} {
				scanned[1..scanned.size-2].replace($\\ ++ $\\, $\\.asString)
			}
		} {
			\unparsed
		};

		debugJSON !? { "< end %: %".format(thisMethod.name, result).debug };

		^result
	}
}

+ Array {
	asJSON { |prettyPrint=false, indent=0, omitFirstIndent=false|
		var indentStr, entriesJoinStr;

		entriesJoinStr = if (prettyPrint, ",\n", ",");

		indentStr = if (prettyPrint, JSON.getIndentation(indent), "");

		^if (omitFirstIndent, "", indentStr) ++
			"[" ++
			if (prettyPrint, "\n", "") ++
			this.collect(_.asJSON(prettyPrint, indent+1)).join(entriesJoinStr) ++
			if (prettyPrint, "\n", "") ++
			indentStr ++
			"]"
	}

	writeAsJSON { |pathname, prettyPrint=false| JSON.writeAsJSON(this, pathname, prettyPrint) }
}

+ Dictionary {
	asJSON { |prettyPrint=false, indent=0, omitFirstIndent=false|
		var indentStr, entriesJoinStr;

		entriesJoinStr = if (prettyPrint, ",\n", ",");

		indentStr = if (prettyPrint, JSON.getIndentation(indent), "");

		^if (omitFirstIndent, "", indentStr) ++
			"{" ++
			if (prettyPrint, "\n", "") ++
			this.keys.asArray.sort({|a, b| a.asString < b.asString}).collect{|key| this.associationAt(key).asJSON(prettyPrint, indent+1)}.join(entriesJoinStr) ++
			if (prettyPrint, "\n", "") ++
			indentStr ++
			"}"
	}

	writeAsJSON { |pathname, prettyPrint=false| JSON.writeAsJSON(this, pathname, prettyPrint) }
}

+ Association {
	asJSON { |prettyPrint=false, indent=0, omitFirstIndent=false|
		var indentStr;

		indentStr = if (prettyPrint, JSON.getIndentation(indent), "");

		^if (omitFirstIndent, "", indentStr) ++
			key.asString.asJSON(prettyPrint) ++
			":" ++
			if (prettyPrint, " ", "") ++
			value.asJSON(prettyPrint, indent, true)
	}
}

+ String {
	asJSON { |prettyPrint=false, indent=0, omitFirstIndent=false|
		var indentStr;

		indentStr = if (prettyPrint, JSON.getIndentation(indent), "");

		^if (omitFirstIndent, "", indentStr) ++
			this.escapeChar($\\).quote;
	}
}

+ Integer {
	asJSON { |prettyPrint=false, indent=0, omitFirstIndent=false|
		var indentStr;

		indentStr = if (prettyPrint, JSON.getIndentation(indent), "");

		^if (omitFirstIndent, "", indentStr) ++
			this.asString;
	}
}

+ Float {
	asJSON { |prettyPrint=false, indent=0, omitFirstIndent=false|
		var indentStr;

		indentStr = if (prettyPrint, JSON.getIndentation(indent), "");

		if ((this == -inf) or: (this == inf)) {
			Error("Infinity is not allowed in JSON.").throw;
		};

		if (this.isNaN) {
			Error("NaN is not allowed in JSON.").throw;
		};

		^if (omitFirstIndent, "", indentStr) ++
			this.asString;
	}
}

+ True {
	asJSON { |prettyPrint=false, indent=0, omitFirstIndent=false|
		var indentStr;

		indentStr = if (prettyPrint, JSON.getIndentation(indent), "");

		^if (omitFirstIndent, "", indentStr) ++
			"true";
	}
}

+ False {
	asJSON { |prettyPrint=false, indent=0, omitFirstIndent=false|
		var indentStr;

		indentStr = if (prettyPrint, JSON.getIndentation(indent), "");

		^if (omitFirstIndent, "", indentStr) ++
			"false";
	}
}

+ Nil {
	asJSON { |prettyPrint=false, indent=0, omitFirstIndent=false|
		var indentStr;

		indentStr = if (prettyPrint, JSON.getIndentation(indent), "");

		^if (omitFirstIndent, "", indentStr) ++
			"null"
	}
}
