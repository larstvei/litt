# The Litt literate system

The literate system Litt attempts to stay very simple. We can only refer
to definitions *or* entire files. This is a rather large limitation,
meaning that we can't have any top-level code that is not a definition,
and we also can't show only parts of a definition. However, this
restriction enforces a style where every block of code that is referred
to has a name (which is given by the definition), and for readability,
it has to stay *small*.

The literate system is implemented in Babashka, a fast native Clojure
scripting runtime. By adding a `bb.edn` file, we set the project up for
using Babashka. In addition, we declare a set of *tasks* that can be run
from the current project; more on those later.

`./bb.edn`{=ref-file}
