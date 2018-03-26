# JSON-sc

JavaScript Object Notation (JSON) support for SuperCollider

## Requirements

Requires StringScanner-sc classes.

This code was developed and have been tested in SuperCollider 3.6.6.

## Installation

Install the [StringScanner-sc](http://github.com/antonhornquist/StringScanner-sc) dependency.

Copy the `JSON-sc` folder to the user-specific or system-wide extension directory. Recompile the SuperCollider class library.

The user-specific extension directory may be retrieved by evaluating `Platform.userExtensionDir` in SuperCollider, the system-wide by evaluating `Platform.systemExtensionDir`.

## Credits

The parser is based on and partially ported from [Ruby code by Florian Frank](http://github.com/flori/json)

## License

Copyright (c) Anton Hörnquist
