# Typesetting

`litt.typesetting`{=ref-def}

I Litt utgjør både tekst og kode et program. Programmet skal kunne
kjøres, men minst like viktig skal det kunne leses. Kildefilene i Litt
består av filer i et markeringsspråk og av helt ordinære kodefiler.
Typesetting av et program i Litt går ut på å inkludere kode fra
kodefiler inn i markeringsspråket, og konvertere dette til et lesbart
format (som antageligvis er HTML eller PDF).

## Pandoc

Litt bruker Pandoc for å behandle markeringsspråket og for å typesette
programmet. Pandoc^[Se <https://pandoc.org>.] kan lese en hel rekke
dokumentformater (som markdown, reStructuredText, LaTeX, HTML, og så
videre), som alle sammen konverteres til en felles intern
representasjon. Denne interne representasjonen er et *abstrakt
syntakstre* for dokumenter. Pandoc kan skrive et abstrakt syntakstre til
en enda lenger liste av formater (som markdown, HTML, LaTeX, PDF, EPUB,
og så videre). Ved å lage et felles format, som er rikt nok til å fange
de fleste strukturene som dukker opp i dokumenter, så kan det fungere
som en slags *alle-til-alle transpilator*.

I tillegg til å kunne oversette mellom mange dokumentformater, så kan
Pandoc både lese og skrive [JSON](https://www.json.org/json-en.html),
som er svært utbredt dataformat. JSON-strukturen som kommer ut av Pandoc
er en representasjon av det abstrakte syntakstreet. Dette gjør Pandoc
unikt *fleksibelt*. Siden nesten alle programmeringsspråk har god støtte
for å lese å skrive JSON, så kan vi bruke hvilket programmeringsspråk vi
vil til å gjøre transformasjoner på det abstrakte syntakstreet!^[Merk at
Pandoc har spesielt god støtte for å utvides på denne måten i
[Lua](https://www.lua.org/) gjennom
[Lua-filtere](https://pandoc.org/lua-filters.html).] Denne mekanismen
kalles [*filtere*](https://pandoc.org/filters.html) i Pandoc. Det er
dette Litt benytter seg av for å kunne sy sammen det litterære innholdet
med koden for å typesette et program. Hvis Litt skulle vært enda mindre,
ville det kun vært et Pandoc filter, og kanskje det hadde vært nok.

`litt.typesetting/call-pandoc`{=ref-def}
