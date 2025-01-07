# A literate language server

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

`litt.lsp`{=ref-def}

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

`litt.lsp/read-message`{=ref-def}

Messages are sent using the same basic structure, that is, a header
containing the content length (in bytes) and the JSON object, separated
by a blank line. Assuming we receive a message as a Clojure data
structure, we print the header and message content encoded as JSON.

`litt.lsp/send-message`{=ref-def}
