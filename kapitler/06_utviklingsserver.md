# Utviklingsserver

Noe av det som gir meg mest programmeringsglede er *interaktive
programmeringsomgivelser*, som er det du får når resultatet av endringer
i koden er *umiddelbart* tilgjengelig. Dette er et kjennemerke for
programmeringsspråkfamilien Lisp, som Clojure er en del av, og for Emacs
(mitt foretrukne utviklingsmiljø). Når du skriver Clojure-kode i Emacs,
så setter du i gang programmet når du begynner å kode, og programmet vil
stort sett holde seg kjørende frem til du gir deg for dagen. Snarere enn
å skrive litt kode, avslutte programmet, kompilere og kjøre, for å så
få programmet tilbake i tilstanden det var i *hver gang du gjør en
endring*, så vil man i Clojure evaluere den nye koden gjennom et
tastetrykk eller to, og *se* programmet endre seg. Du kan glede deg over
å se programmet vokse frem.

Litt gir også en interaktiv programmeringsomgivelse, enn så lenge du
bruker det i kombinasjon med et interaktivt programmeringsspråk og i et
utviklingsmiljø. Dette oppnås gjennom å kontinuerlig typesette boken og
eksponere resultatet på en lokal utviklingsserver.

`litt.serve`{=litt}
