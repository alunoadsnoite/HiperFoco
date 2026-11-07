# HiperFoco

O HiperFoco é um app Android para organizar interesses especiais. Cada assunto ganha um **universo**, e cada universo reúne os **subtemas** que você quer explorar. Os dados ficam no aparelho e o app funciona offline.

Pensado para pessoas autistas, o app ajuda a catalogar seu hiperfoco com recursos de acessibilidade e autorregulação.

## Comece por um universo

1. Na tela **Meus universos**, toque em **Criar universo** e dê um nome ao seu interesse.
2. Toque em **Abrir subtemas** para dividir o assunto em tópicos menores.
3. Use **Buscar universos e subtemas** para encontrar um assunto. Por exemplo, digite `fosseis` para encontrar **Dinossauros** pelo subtema **Fósseis e escavações**.
4. Use **Editar** para renomear um item. Ao confirmar a exclusão de um universo, seus subtemas também são excluídos.

Na primeira abertura, o app traz cinco universos de exemplo. **Linux** inclui terminal, permissões e scripts em Bash. **Dinossauros** inclui períodos, fósseis e o parentesco com as aves. Você pode editar ou excluir os exemplos.

## Recursos de acessibilidade

- **Ícone e cor por universo**: ao criar ou editar um universo, escolha um emoji e uma cor para identificá-lo visualmente de imediato.
- **Nota / descrição nos subtemas**: anote fatos, links e citações dentro de cada subtema para não perder nenhum detalhe do seu interesse.
- **Subtemas concluídos**: marque os subtemas já explorados com o checkbox, dando sensação de progresso e fechamento.
- **Cronômetro de sessão**: ao abrir um universo, inicie o temporizador para acompanhar quanto tempo você está imerso no assunto.
- **Lembretes de autocuidado**: a cada 15 minutos de sessão, o app exibe um aviso suave sugerindo uma pausa, beber água ou alongar.
- **Modo escuro**: alterne entre tema claro e escuro no canto superior direito para reduzir estímulos visuais.
- **Exportar backup**: toque em 💾 para gerar o catálogo em JSON, copiar o texto ou compartilhá-lo para guardar uma cópia dos seus dados.

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
- **Room** para armazenar universos e subtemas no aparelho (com migração automática do banco).
- **ViewModel e StateFlow** para refletir as alterações na tela.
- **Robolectric** para testar a persistência, a exclusão em cascata e o fluxo de criação e edição de subtemas.

## Licença

Distribuído sob a GPL-3.0-or-later. Consulte o arquivo [LICENSE](LICENSE).
