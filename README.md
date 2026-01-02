<p align="center">
<img width="620" height="100" alt="gamelabs-logo-dark" src="https://github.com/user-attachments/assets/4346f635-73d2-4bda-ba65-99a2b865b5fe#gh-light-mode-only" />
<img width="620" height="100" alt="gamelabs-logo-light" src="https://github.com/user-attachments/assets/f5d43f3e-6c21-46f2-9596-78f96692a44d#gh-dark-mode-only" />
</p>

**Gamelabs** é um launcher e front-end para **Android** projetado para transformar seu dispositivo móvel em um **console portátil definitivo**. Com foco total na experiência do usuario, ele organiza jogos e aplicativos em uma interface **fluida, rápida e 100% navegável por controles físicos**.

###  🚀 Preview

<p align="center">
  <img src="https://github.com/user-attachments/assets/8d5080b3-f9f8-4ca1-880e-a09a6956294f8" width="500" />
</p>


## ✨ Funcionalidades

### 🎮 Interface Estilo Console
- Navegação horizontal intuitiva
- Otimizada para **telas em landscape**
- Totalmente compatível com **controles físicos**

### 🕹️ Suporte a Consoles e Apps

#### 🟦 Sony PlayStation 1 (Nativo)
- Motor de execução **interno**
- Suporte aos formatos:
  - `BIN / CUE`
  - `CHD`
  - `PBP`
  - `ISO`

#### 🟪 Sony PlayStation 2 (Externo)
- Integração com emuladores externos:
  - **NetherSX2**
  - **AetherSX2**
- Organização automática das ISOs
- Lançamento direto do emulador pelo Gamelabs

#### 🟥 Sony PlayStation Portable (Externo)
- Integração com o emulador **PPSSPP**
- Suporte a arquivos:
  - `ISO`
  - `CSO`

#### 🤖 Android Games
- Launcher para apps instalados no sistema
- Suporte a **capas customizadas**
- Visual unificado no estilo biblioteca de jogos

### 🎨 Customização de Capas
- Substituição de ícones padrão por capas personalizadas (`.jpg, .png`)
- Visual consistente de **game library**

### 🪟 Overlay Service
- Botão flutuante sobre outros apps
- Permite:
  - Fechar emuladores externos
  - Retornar ao Gamelabs com **1 clique**


## 📂 Estrutura de Arquivos

O Gamelabs organiza seus arquivos no armazenamento interno seguindo este padrão:

```plaintext
/Games/
├── PS1/
│   ├── ROMs/      # Rodados nativamente pelo Gamelabs
│   └── Covers/
├── PS2/
│   ├── ROMs/      # Lançados via Emulador Externo
│   └── Covers/
├── PSP/
│   ├── ROMs/      # Lançados via Emulador Externo
│   └── Covers/
└── ANDROID/
    └── Covers/    # Capas customizadas para apps Android
```

## 🛠️ Requisitos e Instalação
- Emuladores: Para jogar PS2 e PSP, certifique-se de ter os emuladores correspondentes instalados no seu dispositivo.

- Permissões: Conceda permissão de "Acesso a todos os arquivos" e "Sobreposição a outros apps" (Overlay) para que o botão de fechar funcione corretamente.

- Configuração de Hardware: O projeto está configurado especificamente para o botão de ID 110 do GameSir X5 Lite para alternar entre o sistema e o launcher.

## ⚖️ Licença e Créditos
Este projeto foi desenvolvido por Thiago Aciole. O motor de PS1 é nativo do Gamelabs usando a biblioteca `libretro`, enquanto o suporte a PS2 e PSP depende de softwares terceiros.
