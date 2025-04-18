# Definisjoner

I Clojure defineres alle variabler, funksjoner, makroer, og så videre, i
et [*navnerom*](https://clojure.org/reference/namespaces). Konvensjonelt
starter hver Clojure-fil med en navneromsdeklarasjon.

::: {.columns}
`litt.definitions`{=litt}
`litt.definitions-test`{=litt}
:::

Her har vi to navneromsdeklarasjoner, en for implementasjonen og en for
de tilhørende testene. Gjennom teksten kan du regne med at
implementasjonen tilhører navnerommet `litt.definitions` og testene
tilhører navnerommet `litt.definitions-test`.

En navneromsdeklarasjon består av navnet til navnerommet, sammen med
avhengighetene til navnerommet. Den vanligste formen for avhengigheter i
Clojure er *andre navnerom* og de angis i `(:require ...)`. I
navnerommet for definisjoner angir vi kortformer `s` og `e` for
navnerommene `clojure.string` og `edamame.core`, henholdsvis. Et fullt
kvalifisert navn i Clojure består både av et navnerom og navnet som er
oppgitt i definisjonen. For eksempel, er `clojure.string/reverse` det
fulle navnet til funksjonen som reverserer en streng; når vi har angitt
en kortform for et navnerom behandles det synonymt med det fulle navnet
på navnerommet; altså refererer `clojure.string/reverse` og `s/reverse`
til det samme.

## Navngitte kodeblokker

I Litt er det kun mulig å referere til navngitte kodeblokker eller hele
filer. I Clojure, vil si navngitte kodeblokker si en kodeblokk som
utgjør en *definisjon*.

Merk at dette utgjør en betydelig begrensning i Litt. Det er ikke mulig
å vise bruddstykker av en definisjon, og diskutere dette bruddstykket i
den litterære teksten. Litt omfavner denne begrensningen og tar følgende
ståsted: Dersom det er vanskelig å formidle intensjonen med en
definisjon, så bør definisjonen brytes opp i flere definisjoner. Med
andre ord oppfordrer Litt til holde definisjoner små.

For å kunne identifisere definisjonene i en Clojure-fil gjør Litt noen
litt grove antagelser:

- Den første formen i filen definerer et navnerom.
- De følgende formene på toppnivå er definisjoner.
- Det andre elementet i en form på toppnivå er navnet på det som
  defineres.

At den første formen i filen definerer et navnerom er en veletablert
konvensjon som de aller fleste prosjekter følger. Det er viktig å merke
at det ikke er noe i Clojure som krever denne strukturen. Det betyr at
at det finnes gyldige Clojure-programmer som Litt ikke vil behandle på
noe fornuftig vis.

At alle etterfølgende former på toppnivå må være definisjoner utgjør
ingen stor restriksjon, men det gjør at Litt for eksempel ikke egner seg
for skripting.

Den siste antagelsen, som er at andre element i formen angir navnet på
det som defineres, er sant for de fleste måter noe kan defineres i
Clojure. Det er noen unntak, for eksempel `defmethod`, hvor navnet er en
kombinasjon av første og andre element, og `extend-type` som brukes for
å implementere en [protokoll](https://clojure.org/reference/protocols).
Litt implementerer en løsning for `defmethod`, men har for øyeblikket
ingen løsning for `extend-type`.

## Definisjonsnavn

I Litt er et definisjonsnavn representert som et map med inntil tre
nøkler, og tar høyde for tre typer definisjoner:

- Definisjon av et navnerom.
- En definisjon av en variabel, funksjon, makroer, og lignende.
- En definisjon av en metode, definert med `defmethod`.

Et navnerom med navn `ns` er representert ved `{:ns ns}`. En definisjon
av en funksjon `f` i et navnerom `ns` er representert ved `{:ns ns :name
f}`. Definisjoner for variabler, makroer og lignende har samme form;
altså er en definisjon av en variabel `v` i et navnerom `ns`
representert ved `{:ns ns :name v}`.

[Metoder](https://clojure.org/reference/multimethods) følger samme
mønster som andre definisjoner, men inneholder i tillegg en
*dispatch*-verdi. Metoden med navn `m` i et navnerom `ns` med
dispatch-verdi `d` er representert ved `{:ns ns :name m :dispatch d}`.

## Til og fra strenger

For å kunne referere til definisjoner fra de litterærere filene, så
trenger vi en strengrepresentasjon for definisjonsnavnene. Der det er
mulig bruker vi Clojure sin syntaks for fullt kvalifiserte navn.
Navnerom består helt enkelt av navnet til navnerommet; for eksempel er
strengrepresentasjonen av navnerommet `ns` strengen `"ns"`. For andre
definisjoner består strengrepresentasjonen av navnerommet og
definisjonsnavnet, separert med `/`. For eksempel er
strengrepresentasjonen av en definisjon `f` i et navnerom `ns`
representert ved strengen `"ns/f"`.

Metoder definert med `defmethod` kan ikke refereres til direkte, og har
derfor ikke et veldefinert navn i Clojure, og vi må finne på vår egen
representasjon. Vi følger samme struktur for navnerommet og
metodenavnet, men legger til dispatch-verdien separert med `@`. Symbolet
`@` er valgt bortimot vilkårlig, men har fordelen at det ikke kan brukes
som navn i ordinære definisjoner. Dermed vil metoden med navn `m` i et
navnerom `ns` med dispatch-verdi `d` ha strengrepresentasjon `"ns/m@d"`.

Gitt en strengrepresentasjon for en definisjon bygger vi et map med
nøkler `:ns`, `:name` og `:dispatch`. Vi antar at strengen inneholder
maksimalt én forekomst av symbolene `/` og `@`. Strengen deles opp med
hensyn til det regulære uttrykket `#"/|@"`, og de resulterende strengene
gjøres om til symboler. For eksempel vil strengen `"ns/m@d"` resultere i
en vektor med de tre symbolene `[ns m d]`. Til slutt assosieres nøklene
`[:ns :name :dispatch]` med de resulterende symbolene (i den gitte
rekkefølgen).

`litt.definitions/str->definition`{=litt}

Merk at `zipmap` tar en sekvens med nøkler og en sekvens med verdier som
argument, og assosierer første nøkkel med første verdi, andre nøkkel med
andre verdi, og så videre, og terminerer så fort den går tom for nøkler
eller verdier. Det er grunnen til at denne funksjonen håndterer alle
typer definisjoner. La oss spikre det i en liten test.

`litt.definitions-test/str->definition`{=litt}

Gitt et map som representerer en definisjon, med nøkler `:ns`, `:name`
og `:dispatch` som beskrevet tidligere, vil denne funksjonen returnere
strengrepresentasjonen for definisjonen:

`litt.definitions/definition->str`{=litt}

Funksjonen benytter seg av flere små *Clojureismer*. Argumentet til
funksjonen er et map og det
[*destruktureres*](https://clojure.org/guides/destructuring), og binder
nøklene `:ns`, `:name` og `:dispatch` til variablene `ns`, `name` og
`dispatch`. En variabel vil være `nil` dersom nøkkelen ikke er assosiert
med noe i mappet. Funksjonen `str` som lager strenger fra vilkårlig data
gjør `nil` om til en tom streng. For eksempel, hvis `name` er bundet til
`nil`, så vil `(when name "/")` evaluere til `nil`, og dermed vil
hverken `"/"` eller `name` dukke opp i den resulterende strengen. Derfor
støtter også denne funksjonen alle de tre typene definisjoner, som
illustrert i testen under.

`litt.definitions-test/definition->str`{=litt}

## Former til definisjoner

Hver kodeblokk du har sett i dette kapittelet utgjør én *form* i
Clojure. Felles for alle Lisp-er (språkfamilien Clojure er en del av) er
at koden skrives som datastrukturer i språket selv. Parentesuttrykk
`(...)` utgjør lister, hakeparentesuttrykk utgjør `[...]` vektorer og
`{...}` krøllparenteser utgjør maps. Det gjør at det er svært enkelt å
behandle Clojure-kode i Clojure!

Gitt et navnerom og en form, ønsker vi hente ut definisjonsnavnet og
representere det som et map på formen beskrevet over. Merk at vi
utelater former som starter med noe annet enn et symbol, siden det
umulig kan være en form som utgjør en definisjon.^[En vanlig
konstruksjon som brukes på toppnivå, men ikke er en definisjon, er en
`(comment ...)`-blokk, som ofte er nyttig i en interaktiv
programmeringsflyt.]

`litt.definitions/form->definition`{=litt}

Igjen bruker vi destrukturering for å få ut de tre første elementene av
formen som er gitt som input. Funksjonen tar utgangspunkt i `{:ns
ns-name}`, og tilføyer nøkkelen `:name` dersom definisjonen er noe annet
enn en navneromsdeklarasjon, og tilføyer nøkkelen `:dispatch` dersom
formen er en metode. Under er noen enkle tester for funksjonen.

`litt.definitions-test/form->definition`{=litt}



`litt.definitions/definition-info`{=litt}
`litt.definitions/definitions`{=litt}
`litt.definitions/locate-definition-by-name`{=litt}
