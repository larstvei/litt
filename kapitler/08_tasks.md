# Exposing literate features with Babashka tasks

In this section, we pull pieces together to expose the features of our
literate system.

`litt.core`{=litt}

We keep a `config` that can be seen as a set of global parameters.
Anything we want to parameterize should be found within the config. It
specifies where to look for literary files and where to look for source
files, given as glob pattern.

`litt.core/config`{=litt}

An auxiliary function `expand` takes a collection of paths, where each
path is a glob expressions, and returns a list of all files that matches
the glob patterns.

`litt.core/expand`{=litt}

## Literary coverage

With the functions above, we can easily determine out *literary
coverage* of the project. We say that any definition that is referred to
within a literary file has been literarily covered; that is, we assume
that any reference is accompanied by text that documents it. The
function below takes a `config` and displays a report of what
definitions are covered.

`litt.core/report-coverage`{=litt}

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
litt.core                                              4  /  5   ⚠️️
  | list-definitions                                                  ❌
litt.definitions                                       3  /  3   ✅
litt.references                                        2  /  2   ✅
```

## List definitions

As the report suggests, we have yet to discuss `list-definitions`. It
simply lists all the definitions in the project.

`litt.core/list-definitions`{=litt}

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
litt.core/report-coverage
litt.core/list-definitions
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
    (insert "`" definition "`{=litt}")))
```
## Definition info

To get information about a definition we expose the task
`definition-info`, as defined in the following.

`litt.core/definition-info`{=litt}

```example
λ bb definition-info litt.core/definition-info
{:row 37,
 :col 1,
 :end-row 43,
 :end-col 34,
 :file "src/pulse/lit/core.cljc"}
```

## Making them callable from Babashka

`./bb.edn`{=litt-file}
