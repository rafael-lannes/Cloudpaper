# Cloudpaper 🖼️✨

**Cloudpaper** é uma aplicação Android moderna desenvolvida em **Kotlin** e **Jetpack Compose (Material 3)** para automatizar a troca periódica e agendada de papéis de parede a partir de pastas locais do seu dispositivo, com suporte a enquadramento/reposicionamento interativo de imagem.

---

## ✨ Funcionalidades Principais

- 📅 **Trocas Agendadas (Wallpaper por Dia da Semana)**:
  - Defina um wallpaper específico para cada dia da semana (**Segunda a Domingo**).
  - Horário de troca diária configurável (ex: todo dia às 08:00).
  - Visualização semanal com status de "Hoje", miniaturas e opções de troca individual.
  - Fallback inteligente: se algum dia não tiver imagem fixada, o app sorteia da pasta ativa.

- ✂️ **Reposicionamento, Enquadramento & Suporte a Paralaxe**:
  - Editor de enquadramento em tela cheia com visualização panorâmica da imagem e gestos de **pinça (pinch-to-zoom)** e **arrasto (pan)**.
  - **Modo Paralaxe (Rolagem de Telas)**: Preserva a largura panorâmica do wallpaper e aplica dimensões ideais (`suggestDesiredDimensions`), permitindo rolagem fluida entre as telas da launcher do Android.
  - **Modo Fixo**: Enquadramento fixo para uma única tela.
  - Botões de alinhamento rápido: **Centro**, **Esquerda**, **Direita** e **Ver Toda**.
  - Aplicação imediata com o recorte exato na **Tela Inicial**, **Tela de Bloqueio** ou **Ambas as Telas**.
  - Possibilidade de salvar o enquadramento diretamente para qualquer dia da Agenda.

- ⏰ **Troca Automática Periódica por Intervalo**:
  - Gira o wallpaper do dispositivo automaticamente em intervalos configuráveis (15 min, 30 min, 1 hora, 2 horas, 4 horas, 6 horas, 12 horas, 24 horas ou minutos personalizados).
  - Suporte para aplicar na Tela Inicial, Tela de Bloqueio ou Ambas.
  - Restrições inteligentes: opção de trocar apenas quando o aparelho estiver **carregando**.

- 📁 **Seleção Flexível de Pastas Locais**:
  - **Pasta Personalizada do Dispositivo**: Aponte para qualquer pasta de imagens no seu aparelho (Downloads, Imagens, Cartão SD, pasta de câmera, etc.) através do seletor nativo do Android (Storage Access Framework).
  - **Pasta Padrão do Aplicativo**: Utilize a pasta interna isolada do aplicativo (`Pictures/wallpapers`), que não requer permissões especiais.
  - **Botão de Atalho "Abrir Pasta"**: Abre o gerenciador de arquivos padrão diretamente na pasta.
  - **Botão "Copiar Caminho"**: Copia o endereço da pasta com um clique.

- 🖼️ **Galeria & Importação**:
  - Galeria em grade para visualizar todos os papéis de parede da pasta ativa.
  - Pré-visualização em tela cheia de alta qualidade.
  - Botão de aplicação manual imediata e botão direto para enquadrar/reposicionar.
  - Permite importar imagens locais para a pasta do aplicativo.

- ⚡ **100% Offline & Seguro**:
  - Sem necessidade de contas na nuvem, logins, chaves de API ou internet.
  - Todo o processamento, recorte e rotação de wallpapers ocorrem localmente no seu dispositivo.

---

## 🛠️ Tecnologias e Arquitetura

- **Linguagem**: Kotlin
- **Interface**: Jetpack Compose com Material Design 3 e suporte a cores dinâmicas (Material You)
- **Agendamento em Segundo Plano**: Android **WorkManager** (`PeriodicWorkRequest` e `CoroutineWorker`) com persistência após reinicialização do dispositivo (`BOOT_COMPLETED`)
- **Acesso a Pastas e Arquivos**: Android Storage Access Framework (`DocumentFile` / `OpenDocumentTree`) com permissões persistíveis
- **Manipulação de Wallpaper e Recorte**: `android.app.WallpaperManager`, `android.graphics.Matrix` & `android.graphics.Canvas`
- **Carregamento de Imagens**: Coil 3 para Compose
- **Persistência de Configurações**: Jetpack DataStore Preferences com serialização de agenda semanal
- **Concorrência**: Kotlin Coroutines e StateFlow / Flow

---

## 📂 Estrutura do Projeto

```
Cloudpaper/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml
│           ├── res/
│           │   ├── values/
│           │   └── xml/
│           └── java/com/cloudpaper/app/
│               ├── CloudpaperApplication.kt
│               ├── MainActivity.kt
│               ├── data/
│               │   ├── model/
│               │   │   ├── AutoChangeMode.kt
│               │   │   ├── DayOfWeekItem.kt
│               │   │   ├── DailyWallpaperConfig.kt
│               │   │   ├── WallpaperItem.kt
│               │   │   ├── WallpaperSource.kt
│               │   │   ├── WallpaperTarget.kt
│               │   │   └── ScheduleConfig.kt
│               │   ├── preferences/
│               │   │   └── AppPreferences.kt
│               │   └── repository/
│               │       └── WallpaperRepository.kt
│               ├── worker/
│               │   ├── WallpaperChangeWorker.kt
│               │   ├── WorkScheduler.kt
│               │   └── BootReceiver.kt
│               └── ui/
│                   ├── theme/
│                   ├── viewmodel/
│                   │   └── MainViewModel.kt
│                   ├── components/
│                   │   ├── WallpaperCard.kt
│                   │   ├── WallpaperFramingDialog.kt
│                   │   ├── WallpaperPickerBottomSheet.kt
│                   │   ├── TimePickerDialog.kt
│                   │   ├── FullscreenPreviewDialog.kt
│                   │   └── IntervalPickerDialog.kt
│                   └── screens/
│                       ├── MainScreen.kt
│                       ├── HomeScreen.kt
│                       ├── ScheduleScreen.kt
│                       ├── GalleryScreen.kt
│                       └── SettingsScreen.kt
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

---

## 📱 Como Usar o App

1. **Na aba Agenda**:
   - Ative as **Trocas Agendadas** e escolha o horário diário de troca (ex: 08:00).
   - Toque em qualquer dia da semana (Segunda a Domingo) para escolher a foto desejada.
   - Use o botão **Enquadrar** para reposicionar a imagem com zoom e arrasto.
2. **Na aba Início**:
   - Veja o status atual, o wallpaper do dia e toque em **Aplicar Wallpaper de Hoje** ou **Trocar Agora**.
   - Alterne facilmente entre o **Modo Agenda Semanal** e o **Modo por Intervalo**.
3. **Na aba Galeria**:
   - Visualize todas as fotos da pasta ativa. Toque em qualquer uma para abrir em tela cheia, reposicionar/enquadrar ou aplicar.
4. **Na aba Ajustes**:
   - Configure a pasta de origem (pasta padrão do app ou pasta personalizada no celular), preferências de economia de bateria (apenas carregando) e modo de operação.
