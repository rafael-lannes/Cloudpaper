# Cloudpaper 🖼️✨

**Cloudpaper** é uma aplicação Android moderna desenvolvida em **Kotlin** e **Jetpack Compose (Material 3)** para automatizar a troca periódica de papéis de parede a partir de pastas locais do seu dispositivo.

---

## ✨ Funcionalidades Principais

- ⏰ **Troca Automática Periódica**:
  - Gira o wallpaper do dispositivo automaticamente em intervalos configuráveis (15 min, 30 min, 1 hora, 2 horas, 4 horas, 6 horas, 12 horas, 24 horas ou minutos personalizados).
  - Suporte para aplicar na **Tela Inicial**, **Tela de Bloqueio** ou **Ambas as Telas**.
  - Restrições inteligentes: opção de trocar apenas quando o aparelho estiver **carregando**.

- 📁 **Seleção Flexível de Pastas Locais**:
  - **Pasta Personalizada do Dispositivo**: Aponte para qualquer pasta de imagens no seu aparelho (Downloads, Imagens, Cartão SD, pasta de câmera, etc.) através do seletor nativo do Android (Storage Access Framework).
  - **Pasta Padrão do Aplicativo**: Utilize a pasta interna isolada do aplicativo (`Pictures/wallpapers`), que não requer permissões especiais.
  - **Botão de Atalho "Abrir Pasta"**: Abre o gerenciador de arquivos padrão diretamente na pasta.
  - **Botão "Copiar Caminho"**: Copia o endereço da pasta com um clique.

- 🖼️ **Galeria & Importação**:
  - Galeria em grade para visualizar todos os papéis de parede da pasta ativa.
  - Pré-visualização em tela cheia de alta qualidade.
  - Botão de aplicação manual imediata para qualquer imagem.
  - Permite importar imagens locais para a pasta do aplicativo.

- ⚡ **100% Offline & Seguro**:
  - Sem necessidade de contas na nuvem, logins, chaves de API ou internet.
  - Todo o processamento e rotação de wallpapers ocorrem localmente no seu dispositivo.

---

## 🛠️ Tecnologias e Arquitetura

- **Linguagem**: Kotlin
- **Interface**: Jetpack Compose com Material Design 3 e suporte a cores dinâmicas (Material You)
- **Agendamento em Segundo Plano**: Android **WorkManager** (`PeriodicWorkRequest` e `CoroutineWorker`) com persistência após reinicialização do dispositivo (`BOOT_COMPLETED`)
- **Acesso a Pastas e Arquivos**: Android Storage Access Framework (`DocumentFile` / `OpenDocumentTree`) com permissões persistíveis
- **Manipulação de Wallpaper**: `android.app.WallpaperManager`
- **Carregamento de Imagens**: Coil 3 para Compose
- **Persistência de Configurações**: Jetpack DataStore Preferences
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
│                   │   ├── StatusBanner.kt
│                   │   ├── FullscreenPreviewDialog.kt
│                   │   └── IntervalPickerDialog.kt
│                   └── screens/
│                       ├── MainScreen.kt
│                       ├── HomeScreen.kt
│                       ├── FolderSelectScreen.kt
│                       ├── GalleryScreen.kt
│                       └── SettingsScreen.kt
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

---

## 📱 Como Usar o App

1. **Na aba Pastas**:
   - Escolha entre a **Pasta Padrão do App** ou toque em **Selecionar Pasta no Celular** para apontar para qualquer pasta de fotos que você já tenha no seu dispositivo.
2. **Na aba Início**:
   - Veja o status atual e toque em **Trocar Papel de Parede Agora** para testar.
3. **Na aba Galeria**:
   - Visualize todas as fotos da pasta ativa. Toque em qualquer uma para abrir em tela cheia e aplicar manualmente se desejar.
4. **Na aba Ajustes**:
   - Ative a **Troca Automática** e escolha o intervalo desejado (ex: a cada 30 minutos, 1 hora, 2 horas, etc.).
   - Escolha se deseja aplicar na Tela Inicial, Tela de Bloqueio ou Ambas.
