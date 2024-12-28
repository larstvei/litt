# Literate system
## Historical note on Emacs Org mode

In an initial draft of this book was developed using the all-powerful
Org mode for Emacs, using Babel for managing the source code. When
writing a literate program in Org mode, all source code is contained
within source blocks in the markup. The source blocks may be edited in a
dedicated buffer, with the appropriate mode for the programming language
of that source block.

Though this is a very powerful and well-developed system, I never got
comfortable with having the source contained within a the markup.
Editing multiple source blocks had to be edited in separate buffers,
even when they belonged to the same namespace. Furthermore, a lot of
Clojure tooling assumes that the code is located in a conventional file
structure. Most notably, this includes running tests, leading me to
frequently tangling all the source code to a conventional file
structure.

Emacs is wonderfully extensible, so I am sure I could have made extended
Org mode and Babel to work better for my use case. However, this would
have coupled the projects deeply to Emacs. In the end, I decided to
rather have the project be deeply coupled to itself, meaning that the
project could embody its own system for literate programming.

## The Pulse literate system

The literate system of Pulse attempts to stay very simple. We can only
refer to definitions *or* entire files. This is a rather large
limitation, meaning that we can't have any top-level code that is not a
definition, and we also can't show only parts of a definition. However,
this restriction enforces a style where every block of code that is
referred to has a name (which is given by the definition), and for
readability, it has to stay *small*.

The literate system is implemented in Babashka, a fast native Clojure
scripting runtime. By adding a `bb.edn` file, we set the project up for
using Babashka. In the following, we tell Babashka to use have the same
dependencies as that of the project, as outlined in Section [Project
setup](./10_meta.html#project-setup). In addition, we declare a set of
*tasks* that can be run from the current project.

`./bb.edn`{=ref-file}

The system for literate programming in this book is located under the
`pulse.lit` prefix.

## Definitions

`pulse.lit.definitions`{=ref-def}

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

`pulse.lit.definitions/definitions`{=ref-def}

In addition, we provide simple functions to convert a string to a
definition map, and vice versa.

`pulse.lit.definitions/str->definition`{=ref-def}
`pulse.lit.definitions/definition->str`{=ref-def}

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

`pulse.lit.definitions/locate-definition-by-name`{=ref-def}

## References

`pulse.lit.references`{=ref-def}

We want to be able to search all literary files and find references to
definitions. A reference is given as a qualified name to the definition
denoted as follows:

```example
`my.ns/my-definition`{=ref-def}
```

The following function finds all references to definitions within a
file, using a simple regex. It returns a definition, as described above,
with the file in which it originated as metadata.

`pulse.lit.references/references`{=ref-def}

## Book export



## A literate language server

At the time of writing, there is no editor support for this literate
programming system (although this will hopefully have changed by the
time you are reading this). Adding a *language server* will allow all
editors that implement the [Language Server
Protocol](https://microsoft.github.io/language-server-protocol/) (LSP)
to work with the literate system of Pulse. These days, that includes
most editors.

An editor that supports LSP plays the role of a client. It can perform
*remote procedure calls* to the server, meaning that language-specific
tasks can be implemented in an external program. The LSP protocol
supports a wide range of capabilities, but our usage will only use the
most basic ones. We want to be able to get *completions* for references
to source blocks, and we want to be able to *go-to-definition*, that is,
follow a reference to the source code.

Note that the protocol is completely independent of the means of
transportation. The most commonly supported means of communicating are
interprocess communication via `stdin` and `stdout`, and socket
communication. We will use `stdin` and `stdout`, as it seems to be the
most common strategy, and it is arguably the simplest.

`pulse.lit.lsp`{=ref-def}

A message in LSP starts with a header, giving the number of bytes of the
message, and optionally the content type of the message. This is similar
to the HTTP protocol. The content of the message is a JSON object
following the [JSON-RPC](https://www.jsonrpc.org/) protocol, which is a
generic format for remote procedure calls. The header and content is
separated by an empty line (more specifically `\r\n\r\n`).

The `line-seq` function takes a reader (that is, an object from which
bytes can be read), and converts it to a lazy sequence of lines. We
consume the entire header, by consuming lines until an empty line is
found; as we do not actually need the information in the header, we will
discard it completely. We use a JSON library to do the heavy lifting of
reading the JSON object and parsing it into a Clojure data structure. We
will assume that reading the entire JSON object corresponds exactly to
the number of bytes that the content length of the header specified
(even though we disregard it). Note that the function `keyword` is
passed in order to convert the string keys of the JSON object to Clojure
keywords.

`pulse.lit.lsp/read-message`{=ref-def}

Messages are sent using the same basic structure, that is, a header
containing the content length (in bytes) and the JSON object, separated
by a blank line. Assuming we receive a message as a Clojure data
structure, we print the header and message content encoded as JSON.

`pulse.lit.lsp/send-message`{=ref-def}

## Exposing literate features with Babashka tasks

In this section, we pull pieces together to expose the features of our
literate system.

`pulse.lit.core`{=ref-def}

We keep a `config` that can be seen as a set of global parameters.
Anything we want to parameterize should be found within the config. It
specifies where to look for literary files and where to look for source
files, given as glob pattern.

`pulse.lit.core/config`{=ref-def}

An auxiliary function `expand` takes a collection of paths, where each
path is a glob expressions, and returns a list of all files that matches
the glob patterns.

`pulse.lit.core/expand`{=ref-def}

### Literary coverage

With the functions above, we can easily determine out *literary
coverage* of the project. We say that any definition that is referred to
within a literary file has been literarily covered; that is, we assume
that any reference is accompanied by text that documents it. The
function below takes a `config` and displays a report of what
definitions are covered.

`pulse.lit.core/report-coverage`{=ref-def}

This function can be invoked as a Babashka task. The Babashka task can
be invoked by running `bb report-coverage`. In the current state of
Pulse, it reports the following:

```example
λ bb report-coverage
pulse.crdt.gcounter                                         6  /  6   ✅
pulse.crdt.gcounter-test                                    8  /  8   ✅
pulse.crdt.history                                          1  /  1   ✅
pulse.crdt.pncounter                                        7  /  7   ✅
pulse.crdt.pncounter-test                                   8  /  8   ✅
pulse.crdt.properties                                       7  /  7   ✅
pulse.graph                                                 5  /  5   ✅
pulse.graph-test                                            12 / 12   ✅
pulse.lit.core                                              4  /  5   ⚠️️
  | list-definitions                                                  ❌
pulse.lit.definitions                                       3  /  3   ✅
pulse.lit.references                                        2  /  2   ✅
```

### List definitions

As the report suggests, we have yet to discuss `list-definitions`. It
simply lists all the definitions in the project.

`pulse.lit.core/list-definitions`{=ref-def}

```
λ bb list-definitions
...
pulse.crdt.gcounter
pulse.crdt.gcounter/empty-gcounter
pulse.crdt.gcounter/increment
pulse.crdt.gcounter/value
pulse.crdt.gcounter/compare
pulse.crdt.gcounter/merge
...
pulse.lit.core/report-coverage
pulse.lit.core/list-definitions
...
```

Though not very useful in and of itself, it may be useful for searching
through the definitions in the project.

As an example, the following Emacs Lisp function is all that is needed
to simplify adding references in a literary file.

```emacs-lisp
(defun pulse-insert-ref-def ()
  (interactive)
  (let* ((candidates (shell-command-to-string "bb list-definitions"))
         (definition (completing-read "Definition: " (string-lines candidates))))
    (insert "`" definition "`{=ref-def}")))
```
### Definition info

To get information about a definition we expose the task
`definition-info`, as defined in the following.

`pulse.lit.core/definition-info`{=ref-def}

```example
λ bb definition-info pulse.lit.core/definition-info
{:row 37,
 :col 1,
 :end-row 43,
 :end-col 34,
 :file "src/pulse/lit/core.cljc"}
```
