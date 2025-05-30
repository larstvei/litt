# Behandling av kildefiler

Litt behandler to sett med filer:

- Litterære filer -- teksten som utgjør det litterære innholdet.
- Kildefiler -- kildekoden som utgjør det kjørbare programmet.

Dette kapitelet omhandler behandling av innholdet til kildefiler.

## Kildefiler

Litt begrenser seg til å behandle kildekode skrevet i Clojure.^[Merk at
dette forhåpentligvis ikke vil være sant i all fremtid.] I de litterære
filene skal vi kunne skrive om kildekoden som ligger plassert i
kildefiler. I Litt gjøres dette gjennom å referere til biter med kode
ved *navn*. Det gjør at Litt må kunne lese Clojure-filer, og trekke ut
alle syntaktiske strukturer som utgjør en definisjon, og lagre disse på
slik at de lar seg enkelt hente ut igjen.

### Navngitte kodeblokker

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

## Navnerom

I Clojure defineres alle variabler, funksjoner, makroer, og så videre, i
et [*navnerom*](https://clojure.org/reference/namespaces). Konvensjonelt
starter hver Clojure-fil med en navneromsdeklarasjon.

::: {.columns}
`litt.src`{=litt}
`litt.src-test`{=litt}
:::

Her har vi to navneromsdeklarasjoner, en for implementasjonen for
behandling av kildefiler og en for de tilhørende testene. Gjennom
teksten kan du regne med at implementasjonen tilhører navnerommet
`litt.src` og testene tilhører navnerommet `litt.src-test`.

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

## Parsing

Litt trenger å kunne identifisere former på toppnivå og hente ut navn
som defineres. I tillegg ønsker vi å kunne gjøre enkel syntaksfremheving
under typesetting. Dette fordrer at Litt kan kan identifisere
*strukturen* i Clojurekode. Det finnes mange biblioteker som gir oss
dette gratis, men Litt har et ønske om å være både liten og selvstendig,
og det må gjerne koste litt. ^[En Lisp-entusiast vil være tidlig ute med
å påpeke er at Lisp er *homoikonisk*, som vil si at koden er uttrykket
som en datastruktur i språket selv! Dette gjør at det sjeldent er
nødvendig å skulle parse Lisp i Lisp, siden vi kan tolke koden som en
datastruktur direkte (I Clojure gjøres dette med funksjonen
`read-string`). Selv om dette er en sann og fantastisk innvending,
så fjerner også denne tolkningen all kobling til den opprinnelige
strengrepresentasjonen.]

Fordelen er at vi kan dyppe en liten tå i *parsing*, som er et
vidunderlig tema! Når vi som trente lesere ser kode, så ser vi ikke en
lang rekke av tegn. Vi ser symboler, tomrom, nøkkelord, kommentarer og
vi ser verdener som åpnes og lukkes; vi ser *struktur*. Parsing er
kunsten å lære datamaskinen å se det vi ser; eller mer konkret, ta en
nærmest strukturløs streng og, fra den, bygge et tre som fanger
strukturen vi mener strengen innehar.

Vi deler gjerne denne prosessen opp i flere steg. Først slår vi sammen
tegn som hører sammen i det som ofte kalles *leksem*. I prosessen kan vi
tilføye noe informasjon, som gir oss det som ofte kalles *tokens*.
Deretter kan vi strukturere tokens i en trestruktur, som gir opphav til
det vi kan kalle et *konkret syntakstre*. Til slutt kan vi trekke ut det
vesentlige og ende opp med et *abstrakt syntakstre*.

### Oppdeling

For å dele opp tegnene i leksem kan vi bruke en serie med regulære
uttrykk, ett for hver type leksem. Under definerer vi en vektor av par,
der hvert par består en type og det regulære uttrykket som gjenkjenner
denne typen leksem.

`litt.src/lexeme-spec`{=litt}

Regulære uttrykk er notorisk vanskelig å lese, men ikke så aller verst
å skrive. Under følger en beskrivelse av hvert regulære
uttrykk:

`:whitespace`{.keyword}
: I Clojure behandles komma (`,`) som mellomrom. Det regulære uttrykket
  fanger komma, samt ulike typer mellomrom og linjeskift med `\s`.

`:comment`{.keyword}
: Alt etter semikolon (`;`) på en linje behandles som en kommentar. Det
  regulære uttrykket fanger semikolonet, etterfulgt av hva som helst
  *bortsett fra* linjeskift med `[^\n]`.

`:meta`{.keyword}
: Clojure støtter å tilføye symboler og beholdere med
  [*metadata*](https://clojure.org/reference/metadata), som er
  informasjon som bæres med verdien, uten at det påvirker likhet eller
  hashverdier. Ved å bruke symbolet `^`, så vil det som kommer etter
  tolkes som metadata.

`:string`{.keyword}
: En streng består av nesten hva som helst som forekommer mellom to
  anførselstegn (`"`). En streng kan inneholde anførselstegn hvis det
  forekommer direkte etter en omvendt skråstrek, altså `\"`. Vi fanger
  dette med `\\.|[^\"]`, som uttrykker hva som helst som kommer etter en
  omvendt skråstrek (dette inkluderer også et anførselstegn) eller hva
  som helst som ikke er et anførselstegn. Om ikke det skulle være nok,
  så er denne disjunksjonen pakket inn i et parentesuttrykk som innledes
  av `?:`. Dette er fordi parentesuttrykk i regulære uttrykk gjør to
  ting: det både *grupperer* og *fanger*. Betydningen av *gruppering* er
  den samme som parenteser i alle ordinære matematiske disipliner.
  Betydningen av *fanger* er at en kan senere hente ut delstrengen som
  ble fanget av uttrykket innenfor parentesuttrykket. Ved å innlede et
  parentesuttrykk med `?:` ber vi om at det som følger skal grupperes,
  men *ikke* fanges, en såkalt ikke-fangende gruppe. Hvorfor vi ønsker
  å unngå å fange innholdet av strengen blir klarere i neste seksjon.

`:number`{.keyword}
: Tall i Clojure er som i de fleste andre språk, altså litt mer
  kompliserte enn man skulle tro. Vi gjør en forenkling her og ser vekk
  fra tall som inneholder `E`, `N` eller `M` (som alle har en betydning
  i Clojure), samt brøktall. Det regulære uttrykket fanger et valgfritt
  minustegn, etterfulgt av sifre, etterfulgt av et valgfritt punktum med
  noen ytterligere sifre.

`:keyword`{.keyword}
: Et symbol som begynner med kolon (`:`) tolkes som et *nøkkelord* i
  Clojure. Det regulære uttrykket fanger de fleste tegn som forekommer
  etter et kolon, med unntak av blanke, ulike parenteser, anførselstegn
  og semikolon.

`:symbol`{.keyword}
: Symboler følger de samme reglene som nøkkelord, men uten kolon forran.

`:open`{.keyword}
: Strukturen av Clojure-kode er gitt av parentesuttrykk, som kan være
  vanlige runde parenteser `()`, hakeparanteser `[]` eller
  krøllparenteser `{}`. Det regulære uttrykket fanger *åpningen* av et
  parentesuttrykk.

`:close`{.keyword}
: Det regulære uttrykket fanger *lukkingen* av et parentesuttrykk.

### Leksing

Fra leksem kan vi utlede tokens, i en prosess vi kaller *leksing*. Den
går ut på å anvende de regulære uttrykkene ovenfor på en inputstreng, og
utifra hvilket regulært uttrykk som ble gjenkjent, produsere et token.

For å legge ting til rette for leksingen, trekker vi ut type leksem i en
egen vektor:

`litt.src/lexeme-kinds`{=litt}

I tillegg fanger vi de regulære uttrykkene ovenfor i en stor
disjunksjon:

`litt.src/regex`{=litt}

Hvert regulære uttrykk pakkes inn i parenteser. Vi lar disse parentesene
både gruppere og fange, som gjør det enkelt å hente ut hvilket uttrykk
som ble gjenkjent. Clojure sine funksjoner på regulære uttrykk
returnerer et treff, som er en vektor `[match g1 g2 ... gn]`, der
`match` er delstrengen som ble gjenkjent, og gruppene `g1`, `g2`, ...,
`gn` vil enten være `nil` eller delstrengen som ble fanget i gruppen.
Gitt en slik vektor, kan vi hente ut typen som ble gjenkjent ved å telle
antall grupper som resulterte i `nil`, og bruke dette som en indeks inn
i `lexeme-kinds`. Dersom typen er et symbol behandler vi dette spesielt
i funksjonen `symbol-kind` som er beskrevet nedenfor.

`litt.src/lexeme-kind`{=litt}

I Clojure har vi mange symboler som bør utheves, for eksempel
`def`{.special-symbol}, `if`{.macro} og `let`{.macro}. Symbolene
`def`{.special-symbol} og `if`{.special-symbol} kalles spesielle
symboler, fordi de er implementert som primitiver i språket.
`let`{.macro} er ikke et primitivt, men heller en makro. Denne
distinksjonen er ikke viktig for Litt, men vi ønsker å skille disse fra
andre symboler (primært for syntaksfremheving). Det er en liten håndfull
spesielle symboler, som kan identifiseres med den innebygde funksjonen
`special-symbol?`. Makroer kan ikke identifiseres like lett, fordi nye
makroer kan defineres dynamisk. Her nøyer vi oss med å identifisere
*innebygde* makroer, som vi gjør ved å slå opp hva symbolet er bundet
til og se om det bærer metadata som indikerer at det er en makro. Utover
de innebyggede makroene ønsker vi å kunne finne de makroene som
definerer noe; disse begynner tradisjonelt med `def`, for eksempel
`t/deftest`{.macro}. Vi fanger opp disse med det regulære uttrykket
`#"\bdef"`, der `\b` angir en ordgrense. Symboler som hverken er
spesielle eller makroer lar vi være helt ordinære symboler.

`litt.src/symbol-kind`{=litt}

Her er noen eksempler på ulike typer symboler, materialisert i en test:

`litt.src-test/symbol-kind`{=litt}

Et token består av et leksem (delstrengen som ble fanget av det regulære
uttrykket), en type (som beskrevet av `lexeme-type`) og en lokasjon, som
igjen består av start- og sluttindeks. Vi lager en liten hjelpefunksjon
for å lage token som følger:

`litt.src/make-token`{=litt}

Her er en bitteliten test for å illustrere hvordan en token typisk kan
se ut:

`litt.src-test/make-token`{=litt}

Nå ligger alt til rette for å produsere tokens fra en streng. Vi bruker
funksjonen `re-seq` for å finne alle suksessive treff av det regulære
uttrykket som fanger alle typer leksem. Fra treffene henter vi ut hvert
leksem (som er delstrengen som ble gjenkjent) og deres respektive type
med `lexeme-kind`. I tillegg beregner vi hvor hvert leksem begynner og
slutter. Til slutt samler vi all informasjonen i et token per treff.

`litt.src/lex`{=litt}

Merk at vi i beregningen av start- og sluttposisjonene bruker funksjonen
`reductions`, som kanskje fortjener en kort forklaring. Der funksjonen
`reduce` anvender en funksjon på hvert element og akkumulerer
resultatet, gir `reductions` en sekvens med hvert delresultat fra
reduksjonen. I dette tilfellet ser vi på lengden av hvert leksem, og tar
summen med initialverdi `0`. Det gir oss en sekvens som starter med `0`,
deretter lengden på første leksem, deretter summen av første og andre
leksem, og så videre, hvor siste siste tall i sekvensen svarer til
lengden av inputstrengen. Hver sluttposisjon svarer til startposisjonen
til neste leksem.

La oss se hvordan resultater på et kall på `lex` typisk ser ut. Vi
sjekker at den tomme strengen gir en tom liste med tokens, og at `"()"`
gir oss en åpneparentes og en lukkeparentes, og at tokene ser fornuftig
ut.

`litt.src-test/lex-basic`{=litt}

Siden denne funksjonen syr sammen en del funksjonalitet kan det være
nyttig å teste den litt nøyere. Vi tar utgangspunkt i en streng `s` som
inneholder de fleste type tokens. Under sjekker vi at typene til de fire
første og siste tokene stemmer overens med det vi forventer. I tillegg
tar vi en stikkprøve på at det første tokenet har lokasjon som starter
på posisjon 0, og at det siste tokenet har en lokasjon som svarer til
lengden på strengen. Til slutt sjekker vi at dersom vi konkatenerer alle
leksemene, så blir resultatet det samme som den opprinnelige strengen.

`litt.src-test/lex-example`{=litt}

### Parsing

`litt.src/parse`{=litt}

## Definisjonsnavn

I Litt er et definisjonsnavn representert som et map med inntil tre
nøkler, og tar høyde for tre typer definisjoner:

- Definisjon av et navnerom.
- En definisjon av en variabel, funksjon, makro og lignende.
- En definisjon av en metode, definert med `defmethod`.

Et navnerom med navn `ns` er representert ved `{:ns ns}`. En definisjon
av en funksjon, variabler, makroer eller lignende med navn `v` i et
navnerom `ns` er representert ved `{:ns ns :name v}`.

[Metoder](https://clojure.org/reference/multimethods) følger samme
mønster som andre definisjoner, men inneholder i tillegg en
*dispatch*-verdi. Metoden med navn `m` i et navnerom `ns` med
dispatch-verdi `d` er representert ved `{:ns ns :name m :dispatch d}`.

### Til og fra strenger

For å kunne referere til definisjoner fra de litterære filene, så
trenger vi en strengrepresentasjon for definisjonsnavnene. Der det er
mulig bruker vi Clojure sin syntaks for fullt kvalifiserte navn.
Navnerom består helt enkelt av navnet til navnerommet; for eksempel er
strengrepresentasjonen av navnerommet `ns` strengen `"ns"`. For andre
definisjoner består strengrepresentasjonen av navnerommet og
definisjonsnavnet, separert med `"/"`. For eksempel er
strengrepresentasjonen av en definisjon `f` i et navnerom `ns`
representert ved strengen `"ns/f"`.

Metoder definert med `defmethod` kan ikke refereres til direkte, og har
derfor ikke et veldefinert navn i Clojure, og vi må finne på vår egen
representasjon. Vi følger samme struktur for navnerommet og
metodenavnet, men legger til dispatch-verdien separert med `"@"`.
Symbolet `@` er valgt bortimot vilkårlig, men har fordelen at det ikke
kan brukes som navn i ordinære definisjoner. Dermed vil metoden med navn
`m` i et navnerom `ns` med dispatch-verdi `d` ha strengrepresentasjon
`"ns/m@d"`.

Gitt en strengrepresentasjon for en definisjon bygger vi et map med
nøkler `:ns`, `:name` og `:dispatch`. Vi antar at strengen inneholder
maksimalt én forekomst av symbolene `/` og `@`. Strengen deles opp med
hensyn til det regulære uttrykket `#"/|@"`, og de resulterende strengene
gjøres om til symboler. For eksempel vil strengen `"ns/m@d"` resultere i
en vektor med de tre symbolene `[ns m d]`. Til slutt assosieres nøklene
`[:ns :name :dispatch]` med de resulterende symbolene (i den gitte
rekkefølgen).

`litt.src/str->definition-name`{=litt}

Merk at `zipmap` tar en sekvens med nøkler og en sekvens med verdier som
argument, og assosierer første nøkkel med første verdi, andre nøkkel med
andre verdi, og så videre, og terminerer så fort den går tom for nøkler
eller verdier. Det er grunnen til at denne funksjonen håndterer alle
typer definisjoner. La oss spikre det i en liten test.

`litt.src-test/str->definition-name`{=litt}

Gitt et map som representerer en definisjon, med nøkler `:ns`, `:name`
og `:dispatch` som beskrevet tidligere, vil denne funksjonen returnere
strengrepresentasjonen for definisjonen:

`litt.src/definition-name->str`{=litt}

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

`litt.src-test/definition-name->str`{=litt}

### Former til definisjoner

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

`litt.src/extract-definition-name`{=litt}

Igjen bruker vi destrukturering for å få ut de tre første elementene av
formen som er gitt som input. Funksjonen tar utgangspunkt i `{:ns
ns-name}`, og tilføyer nøkkelen `:name` dersom definisjonen er noe annet
enn en navneromsdeklarasjon, og tilføyer nøkkelen `:dispatch` dersom
formen er en metode. Under er noen enkle tester for funksjonen.

`litt.src-test/extract-definition-name`{=litt}

## Hente ut informasjon om en definisjon

Så langt har vi hentet ut navnet til definisjonen, men vi ønsker også å
holde oversikt over hvilken fil definisjonen kommer fra, og den faktiske
definisjonsteksten. Denne funksjonen gjør en del antagelser om input.
Den forventer å få filen som definisjonen forekommer i, linjene i filen
(som en vektor av strenger) og en *form*. I tillegg forventer den at
formen er produsert av biblioteket
[Edamame](https://github.com/borkdude/edamame), som lagrer lokasjonsdata
om formen som [*metadata*](https://clojure.org/reference/metadata). Kort
forklart er metadata informasjon som kan legges til en vilkårlig verdi,
uten at det påvirker likhet eller hashkoder; altså er to like verdier
med *ulik* metadata, fremdeles like.

`litt.src/definition-info`{=litt}

Her henter vi ut linjene hvor definisjonen starter og slutter fra
metadaten til formen. Vi returnerer et map som inneholder filen
definisjonen kommer fra, linjenummeret definisjonen starter på og
linjene som utgjør definisjonen. Under er et eksempel på et kall på
`definition-info` utformet som en enkel test.

`litt.src-test/definition-info`{=litt}

## Samle definisjoner

Med funksjonene over, er vi klare til å kunne ta en Clojure-fil og samle
opp alle definisjonene i filen. Vi representerer filer med et map med
nøklene `:file/filename` som gir filnavnet og `:file/content` som gir
innholdet som en streng. Resultatet av å samle definisjonene er et map
der nøklene er definisjonsnavn, og verdien er informasjon om
definisjonen.

`litt.src/definitions`{=litt}

Funksjonen parser filens innhold med Edamame, som resulterer i en vektor
av former.^[Edamame kan både brukes til å parse
[edn](https://github.com/edn-format/edn), som er et subset av Clojure
sin syntaks, og Clojure-kode. Ved å gi argumentet `{:all true}`
forteller vi Edamame at vi ønsker å parse alle syntaktiske strukturer i
Clojure.] Vi fisker ut navnet på navnerommet, som antatt å være det
andre elementet i den første formen. Siden innholdet er gitt som en
streng (og ikke en sekvens av linjer)^[Antageligvis bør vi representere
innholdet til en fil som en vektor av strenger, slik at vi slipper denne
konverteringen frem og tilbake!] splitter vi den opp i en vektor med en
streng per linje.

Til slutt bygger vi et map fra formene med en `reduce`. Nøkkelen
produseres med funksjonen `extract-definition-name` som er beskrevet
over. Husk at denne funksjon kan returnere `nil` dersom det ikke er noe
definisjonsnavn å trekke ut! I så fall legger vi heller ikke til denne
formen i resultatet. Dersom definisjonsnavnet blir hentet ut uten
problemer, assosierer vi definisjonsnavnet med informasjonen om
definisjonen med `definition-info`, som beskrevet ovenfor.

Til slutt skriver vi noen tester for denne funksjonen. Vi representerer
hver fil med et filnavn og en streng som innhold. Den første er tom, og
bør gi et tomt map i retur. Den neste inneholder kun en
navneromdeklarasjon, som bør gi et map med definisjonsnavnet til
navnerommet og informasjonen om det. Til slutt har vi en fil med en
navneromsdeklarasjon og to funksjonsdefinisjoner, som bør gi et map som
assosierer tre definisjonsnavn med informasjonen om dem.

`litt.src-test/definitions`{=litt}
