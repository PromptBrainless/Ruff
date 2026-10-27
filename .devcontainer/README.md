# Ruff-Entwicklungscontainer

Der Container stellt JDK 17, Python 3.12 und die vorab festgelegten
Entwicklungsprüfungen bereit. `ANDROID_HOME` und `ANDROID_SDK_ROOT` zeigen auf
`/usr/local/lib/android`.

Das Android SDK und dessen API-35-Plattform werden nicht automatisch installiert
oder lizenziert: Vor der Installation muss eine verantwortliche Person die
offiziellen Android-SDK-Bedingungen prüfen und ausdrücklich annehmen. Nach
Installation der offiziellen Android SDK Tools beziehungsweise des Android
CLI im gesetzten SDK-Pfad können die Pakete installiert werden:

```bash
android sdk install platforms/android-35 build-tools/35.0.0 platform-tools
```

Android CLI ist laut offizieller Dokumentation der aktuelle Weg; das ältere
`sdkmanager` ist dort als veraltet markiert. Alle Lizenzabfragen sind
interaktiv zu beantworten; keine automatische Lizenzannahme per `yes`
verwenden. Danach lässt sich der Build mit `./gradlew :app:assembleDebug`
ausführen.
