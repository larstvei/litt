# Definitions

`litt.definitions`{=ref-def}

In order to refer to definitions, we first need a way to identify them.
Here we some crude assumptions: the first form is a namespace
declaration, and every top-level form is a definition, where the second
element of the form is the name of what is defined. That every top-level
form is a definition is a convention we have restricted ourselves to
follow (as mentioned above). That the second element of a form gives the
name of what is defined seems to be a well-established convention in
Clojure (and perhaps all Lisps). There are at least no counter examples
of this in Pulse.

The following function gathers all definitions in a source file, and
returns a map of the form `{:ns my.ns :name my-definition}`. It does so
by parsing the source file using `edemame`, which conveniently adds
metadata with the location (line and column) of the parsed expression.
We keep this metadata, and add the the originating file.

`litt.definitions/definitions`{=ref-def}

In addition, we provide simple functions to convert a string to a
definition map, and vice versa.

`litt.definitions/str->definition`{=ref-def}
`litt.definitions/definition->str`{=ref-def}

When extracting definitions from source files, we also store valuable
information as metadata, namely where the definition is stored. We
create this, perhaps strange-looking, function that finds a definition
by its name. What makes it a bit strange, is that `definition->str`
already gives us a definition from its name, but this definition lacks
metadata. The first argument `defs` is a assumed to be a set of
definitions. A Clojure set doubles as a function that returns `nil` when
given a value that is not in the set, and returns the value if it is in
the set. An important detail here, is that it returns the instance of
the value that is in the set, meaning that it carries along the
metadata.

`litt.definitions/locate-definition-by-name`{=ref-def}
