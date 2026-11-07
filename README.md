# HiperFoco

O HiperFoco é um app Android para organizar interesses especiais. Cada assunto ganha um **universo**, e cada universo reúne os **subtemas** que você quer explorar. Os dados ficam no aparelho e o app funciona offline.

## Comece por um universo

1. Na tela **Meus universos**, toque em **Criar universo** e dê um nome ao seu interesse.
2. Toque em **Abrir subtemas** para dividir o assunto em tópicos menores.
3. Use **Buscar universos e subtemas** para encontrar um assunto. Por exemplo, digite `fosseis` para encontrar **Dinossauros** pelo subtema **Fósseis e escavações**.
4. Use **Editar** para renomear um item. Ao confirmar a exclusão de um universo, seus subtemas também são excluídos.

Na primeira abertura, o app traz cinco universos de exemplo. **Linux** inclui terminal, permissões e scripts em Bash. **Dinossauros** inclui períodos, fósseis e o parentesco com as aves. Você pode editar ou excluir os exemplos.

## Compile e teste

Use **Android Studio**, **JDK 21** e **Android SDK 37**. O app roda a partir do **Android 8.0 (API 26)**.

Abra a raiz do projeto no Android Studio, aguarde a sincronização do Gradle e selecione um aparelho ou emulador. Para compilar e testar pelo terminal, execute os comandos abaixo na raiz do projeto.

```sh
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

No Windows, use `gradlew.bat` no lugar de `./gradlew`. A primeira compilação baixa as dependências do projeto. O APK de depuração é gerado em `app/build/outputs/apk/debug/app-debug.apk`.

## Como o app é feito

- **Kotlin e Jetpack Compose** para a interface.
- **Room** para armazenar universos e subtemas no aparelho.
- **ViewModel e StateFlow** para refletir as alterações na tela.
- **Robolectric** para testar a persistência, a exclusão em cascata e o fluxo de criação e edição de subtemas.

## Licença

Distribuído sob a GPL-3.0-or-later. Consulte o arquivo [LICENSE](LICENSE).
