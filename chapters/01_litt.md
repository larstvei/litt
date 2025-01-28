# Litt -- Et litterært programmeringssystem

*Litt* er et system for «litterær programmering» (eng: *Litterate
programming*). Et litterært program [@knuth1984] er like fullt et
*litterært verk* som skal leses og forstås av deg, som et *program* som
skal kunne instruere en datamaskin om hva den skal gjøre.

Det hadde vært nærmest pinlig om Litt ble utviklet som et ikke-litterært
program.^[Merk at vi på norsk går glipp av den engelske motsatsen til
litterær programmering: *illiterate*.] Så det du leser nå er *Den lille
boken om Litt*. Den inneholder kildekoden for programmet, forteller
historien om programmet, og gir teorien om programmet [@naur1985].

## Litt om Litt

Noe som (såvidt jeg vet) skiller Litt fra andre systemer for litterær
programmering er at det litterære materiale holdes avskilt fra koden.
Det vil si at det litterære materialet skrives i egne filer i et
markeringsspråk og koden skrives i ordinære kodefiler. På et vis bryter
dette noe med tanken om litterær programmering. I tradisjonell litterær
programmering er mye poenget at teksten og koden er vevd sammen; det er
en og samme aktivitet.

Litt forsøker ikke å bryte med dette, og vil etter beste evne prøve
å leve etter idealene som tradisjonell litterær programmering
etterstreber, men nærmer seg det fra et annet hold. Snarere enn å gjøre
denne sammenvevingen gjennom å plassere innholdet i de samme filene, vil
Litt heller være et verktøy som kan brukes til å styrke koblingen mellom
innhold som ligger i separate filer. Langt mer konkret, så eksponerer
Litt en LSP-server som bidrar med å skape denne koblingen; hva en
LSP-server er, og hvordan den bidrar med denne koblingen er temaet for
[Kapittel 6](/chapters/06_lsp.html).

Motivasjonen for å holde det litterære materiale adskilt fra koden er:

- Kode inneholdt i et markeringsspråk medfører et byggesteg (altså,
  trekk koden ut før evaluering eller kompilering).
- God editorstøtte for litterærere systemer er stort sett forbeholdt den
  eneste sanne editoren.^[Se [Org Mode](https://orgmode.org/) for
  [Emacs](https://www.gnu.org/software/emacs/).]
- Veldig mange verktøy for programvareutvikling hviler på antagelsen om
  at kode ligger i en konvensjonell filstruktur.
- Ikke mange har erfaring med litterær programmering, som kan heve
  terskelen for å bidra til et litterært programmeringsprosjekt.
- Det er enklere å postlitterærisere et program.

Etter min erfaring oppleves Litt litt mindre *inngripende* enn andre
systemer for litterær programmering. Merk at dette er basert på relativt
lite erfaring, ettersom jeg kun har skrevet en håndfull litterære
programmer, og ingen ferdigstilte i Litt, siden Litt er bare litt av hva
Litt skal bli i skrivende stund.

I Litt skapes kobligen mellom kode og tekst gjennom referanser til
*navngitte* blokker med kode fra markeringsspråket. Vi belager oss
på programmeringsspråkets egne mekanismer for å gi navn (som for
eksempel funksjonsdefinisjoner), snarere enn å referere til linjenummere
(som krever enorme mengder vedlikehold) eller å skulle legge til
Litt-spesifikke kommentarer i koden. Et eksempel på en referanse til en
funksjon i programmeringsspråket Clojure kan se slik ut:

```
`litt.db/config`{=ref-def}
```

Her refereres det til en definisjon av `config` som ligger i navnerommet
`litt.db`. Når dette skrives inn i markeringsteksten, så vil koden
settes inn i den typesatte boken. Gitt at du leser den typesatte boken,
så vil det vil resultatet se slik ut:

`litt.db/config`{=ref-def}

I Donald Knuths originale WEB system for strukturert dokumentasjon
[@knuth1983a], beskriver han de to definerende programmene for det han
året etter døpte *Litterate programming*:

> Besides providing a documentation tool, WEB enhances the PASCAL
> language by providing a rudimentary macro capability together with the
> ability to permute pieces of the program text, so that a large system
> can be understood entirely in terms of small modules and their local
> interrelationships. The TANGLE program is so named because it takes a
> given web and moves the modules from their web structure into the
> order required by PASCAL; the advantage of programming in WEB is that
> the algorithms can be expressed in "untangled" form, with each module
> explained separately. The WEAVE program is so named because it takes a
> given web and intertwines the TeX and PASCAL portions contained in
> each module, then it knits the whole fabric into a structured
> document. (Get it? Wow.) Perhaps there is some deep connection here
> with the fact that the German word for "weave" is "web"? and the
> corresponding Latin imperative is "texe"!

Vakkert! Merk at Knuth fremhever WEB sine makrofasiliteter, som gjør at
du kan organisere kode på en måte som er mer fleksibel en med ordinær
PASCAL. Siden vi i Litt kun kan refere til navngitte kodeblokker, så gir
ikke Litt noen mulighet til å påvirke koden fra markeringsspråket. Vi
kan riktignok presentere kodeblokkene i en annen rekkefølge enn de står
oppført i koden, men kan ikke sammenlignes med fleksibiliteten WEB
tilbyr. Det føles imidlertid ikke som et stort offer, ettersom at mer
moderne språk som regel har bedre mekanismer for modulærisering enn hva
Pascal kunne tilby på 80-tallet.

## Litt teknisk

Jeg er nødt til å holde Litt lite, ellers blir det aldri skrevet. Det er
skrevet i og for programmeringsspråket [Clojure](https://clojure.org/)
[@hickey2008; @hickey2020]. Det er mange egenskaper ved Clojure som gjør
det godt egnet for dette prosjektet, og her er noen av dem:

- Det er forholds enkelt og lite.
- Det er helt utrolig enkelt å parse.
- Jeg elsker å tenke og skrive i Clojure.

Det siste er selvfølgelig det viktigste.

Den lille boken om Litt vil ikke gi en god innføring i Clojure.
Forhåpentligvis vil du kunne lese innholdet og forstå flyten, strukturen
og trekke ut teorien om programmet, selv uten forkunnskaper om Clojure.




