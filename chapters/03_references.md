# References

`litt.references`{=ref-def}

We want to be able to search all literary files and find references to
definitions. A reference is given as a qualified name to the definition
denoted as follows:

```example
`my.ns/my-definition`{=ref-def}
```

The following function finds all references to definitions within a
file, using a simple regex. It returns a definition, as described above,
with the file in which it originated as metadata.

`litt.references/references`{=ref-def}
