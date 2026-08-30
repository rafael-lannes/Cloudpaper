# Cloudpaper ☁️🖼️

**Cloudpaper** é uma aplicação Android moderna desenvolvida em **Kotlin** e **Jetpack Compose (Material 3)** projetada para automatizar a rotação e sincronização periódica de papéis de parede a partir do **Google Drive** e de pastas locais **offline**.

---

## ✨ Funcionalidades Principais

- ⏰ **Troca Automática Periódica**:
  - Gira o wallpaper do dispositivo automaticamente em intervalos configuráveis (15 min, 30 min, 1 hora, 2 horas, 4 horas, 6 horas, 12 horas, 24 horas ou minutos personalizados).
  - Suporte para aplicar na **Tela Inicial**, **Tela de Bloqueio** ou **Ambas**.
  - Restrições inteligentes: opção de trocar apenas conectado ao **Wi-Fi** ou quando o aparelho estiver **carregando**.

- ☁️ **Integração com Google Drive**:
  - Conecte sua conta Google com um clique.
  - Informe o link completo ou o ID de qualquer pasta do Google Drive contendo seus papéis de parede.
  - Baixa e escolhe wallpapers aleatórios da nuvem.

- 🔄 **Sincronismo Nuvem ➔ Offline**:
  - Ao acessar o Google Drive, o Cloudpaper sincroniza novos arquivos para o armazenamento offline interno do aparelho.
  - Sincronização incremental: baixa apenas imagens novas sem desperdiçar franquia de dados.
  - **Resiliência Offline**: Se o dispositivo ficar sem internet, a rotação de papéis de parede continua funcionando normalmente utilizando a pasta offline sincronizada.

- 📁 **Galeria Offline & Pasta Local de Wallpapers**:
  - Exibe com clareza o caminho exato onde os papéis de parede ficam salvos no aparelho (`Armazenamento Principal > Android > data > com.cloudpaper.app > files > Pictures > wallpapers`).
  - **Botão de Atalho "Abrir Pasta"**: Abre o gerenciador de arquivos do celular diretamente na pasta offline.
  - **Botão "Copiar Caminho"**: Copia o caminho absoluto para a área de transferência com um clique.
  - Galeria em grade para visualizar todos os papéis de parede salvos no aparelho.
  - Pré-visualização em tela cheia com alta qualidade.
  - Permite importar imagens da galeria do celular para a pasta de rotação offline.
  - Botão de aplicação manual imediata para qualquer imagem.

---

## 🛠️ Tecnologias e Arquitetura

- **Linguagem**: Kotlin
- **Interface**: Jetpack Compose com Material Design 3 e suporte a cores dinâmicas (Material You)
- **Agendamento em Segundo Plano**: Android **WorkManager** (`PeriodicWorkRequest` e `CoroutineWorker`) com persistência após reinicialização do dispositivo (`BOOT_COMPLETED`)
- **Manipulação de Wallpaper**: `android.app.WallpaperManager`
- **Carregamento de Imagens**: Coil 3 para Compose
- **Persistência de Configurações**: Jetpack DataStore Preferences
- **API Google Drive**: Google Drive REST API v3 com Google Play Services Auth
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
│           └── java/com/cloudpaper/app/
│               ├── CloudpaperApplication.kt
│               ├── MainActivity.kt
│               ├── data/
│               │   ├── model/
│               │   │   ├── WallpaperItem.kt
│               │   │   ├── WallpaperSource.kt
│               │   │   ├── WallpaperTarget.kt
│               │   │   ├── ScheduleConfig.kt
│               │   │   └── SyncResult.kt
│               │   ├── preferences/
│               │   │   └── AppPreferences.kt
│               │   ├── drive/
│               │   │   ├── GoogleDriveService.kt
│               │   │   └── DriveAuthHelper.kt
│               │   └── repository/
│               │       └── WallpaperRepository.kt
│               ├── worker/
│               │   ├── WallpaperChangeWorker.kt
│               │   ├── DriveSyncWorker.kt
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
│                       ├── DriveSyncScreen.kt
│                       ├── GalleryScreen.kt
│                       └── SettingsScreen.kt
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

---

## 🚀 Como Executar no Android Studio

1. Abra o **Android Studio**.
2. Selecione **Open** e aponte para a pasta `d:\Gamedev\Projetos\Cloudpaper`.
3. O Android Studio sincronizará o projeto automaticamente através do Gradle.
4. Conecte um dispositivo Android via USB (com Depuração USB ativada) ou inicie um Emulador Android.
5. Clique no botão **Run** (`Shift + F10`) no Android Studio.

---

## 🔑 Configuração do Google Drive API (OAuth 2.0)

Para habilitar a autenticação completa com a sua conta Google no Google Drive:

1. Acesse o [Google Cloud Console](https://console.cloud.google.com/).
2. Crie um novo projeto (ex: `Cloudpaper App`).
3. Em **APIs e Serviços** > **Biblioteca**, pesquise por **Google Drive API** e clique em **Ativar**.
4. Em **Tela de consentimento OAuth**, configure como **Externo** e adicione o escopo `.../auth/drive.readonly`.
5. Em **Credenciais** > **Criar Credenciais** > **ID do cliente OAuth**:
   - Tipo de aplicativo: **Android**
   - Nome do pacote: `com.cloudpaper.app`
   - Impressão digital SHA-1 do certificado: Obtenha a SHA-1 da sua chave de desenvolvimento executando:
     ```bash
     keytool -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
     ```
6. Salve a credencial. O app agora estará pronto para autenticar e listar os arquivos da sua pasta do Drive!

---

## 📱 Como Usar o App

1. **Na aba Google Drive**:
   - Toque em **Conectar com Google** para autorizar sua conta.
   - Cole o link de compartilhamento da pasta do Drive (ex: `https://drive.google.com/drive/folders/...`) e toque em **Salvar Pasta**.
   - Toque em **Sincronizar Agora com Google Drive** para fazer o download das imagens para o seu aparelho.
2. **Na aba Início**:
   - Escolha o modo de rotação: *Pasta Local Offline*, *Google Drive (Nuvem)* ou *Google Drive + Sincronismo Offline*.
   - Toque em **Trocar Papel de Parede Agora** para testar imediatamente.
3. **Na aba Galeria**:
   - Visualize todos os papéis de parede salvos offline.
   - Toque em qualquer imagem para abrir em tela cheia e aplicar manualmente.
   - Toque em **Adicionar** para importar fotos do seu próprio aparelho para a lista de rotação.
4. **Na aba Ajustes**:
   - Ative a chave **Troca Automática**.
   - Selecione a frequência (ex: a cada 30 minutos, 1 hora, etc.).
   - Escolha a tela de destino (Tela Inicial, Bloqueio ou Ambas).
