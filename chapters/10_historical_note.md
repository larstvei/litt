# Historical note on Emacs Org mode

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
