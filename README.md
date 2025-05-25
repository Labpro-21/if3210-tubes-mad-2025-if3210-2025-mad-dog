# Purrytify

Purrytify adalah aplikasi Android yang dibangun sebagai klon dari Spotify, menawarkan fitur-fitur inti seperti login pengguna, kemampuan untuk mengunggah dan mengelola lagu lokal, pemutar media yang lengkap dengan kontrol playback dan antrian, mini player untuk kontrol persisten, layar detail lagu, navigasi skip, fungsionalitas favorit dan riwayat putar, serta antarmuka pengguna modern dan responsif yang terinspirasi oleh desain Spotify dan dibangun dengan Jetpack Compose, termasuk pemutaran latar belakang untuk pengalaman mendengarkan yang mulus.

## Features

- **User Authentication**: Secure login and user profile management
- **Local Music Management**: Upload and manage your local music library
- **Media Playback**: Full-featured media player with playback controls and queue management
- **Mini Player**: Persistent playback controls while navigating the app
- **Song Details**: Detailed view for songs with metadata and controls
- **Favorites**: Mark and manage your favorite songs
- **Play History**: Track your listening history
- **Background Playback**: Continue listening while using other apps
- **Responsive Design**: Modern UI that adapts to different screen orientations
- **Location Features**: Map integration for music discovery
- **Social Features**: QR code sharing and social connectivity
- **Statistics**: Track your listening habits and preferences

## Screenshots

### Core Features

| Home Screen | Library View | Profile |
|-------------|--------------|----------|
| ![Home Screen](screenshots/milestone2/home1.jpg) | ![Library](screenshots/milestone2/library.jpg) | ![Profile](screenshots/milestone2/profile1.jpg) |

### Music Experience

| Album View | Song Details | Audio Controls |
|------------|--------------|----------------|
| ![Album](screenshots/milestone2/album3.jpg) | ![Song Detail](screenshots/milestone2/online_song_detail1.jpg) | ![Audio](screenshots/milestone2/audio_routing.jpg) |

### User Features

| Profile Management | Statistics | Notifications |
|-------------------|------------|---------------|
| ![Edit Profile](screenshots/milestone2/edit_profile.jpg) | ![Stats](screenshots/milestone2/stats.jpg) | ![Notifications](screenshots/milestone2/notification.jpg) |

### Additional Features

| Location Services | Social Sharing | Profile Details |
|------------------|----------------|-----------------|
| ![Map](screenshots/milestone2/map.jpg) | ![QR Share](screenshots/milestone2/qr_share.jpg) | ![Profile Details](screenshots/milestone2/profile2.jpg) |

### Responsive Design

| Landscape View 1 | Landscape View 2 |
|-----------------|-----------------|
| ![Responsive 1](screenshots/milestone2/responsive1.jpg) | ![Responsive 2](screenshots/milestone2/responsive2.jpg) |

## Technology Stack

### Android Framework & UI
- Jetpack Compose
- Material Design 3
- Android Core KTX
- Android Lifecycle Components
- Navigation Component
- SplashScreen API

### Data Management
- Room Persistence Library
- DataStore
- Security-Crypto
- WorkManager

### Media & Graphics
- ExoPlayer (Media Playback)
- Coil (Image Loading)
- Palette KTX (Color Extraction)

### Networking & API
- Retrofit
- OkHttp
- Gson Converter
- Kotlin Coroutines

## Project Structure

- **UI Layer**:
  - Compose UI Components
  - Material 3 Design System
  - Custom Themes and Styling
  - Navigation Components

- **Data Layer**:
  - Room Database
  - Repository Pattern
  - DataStore Preferences
  - Network Services

- **Domain Layer**:
  - Use Cases
  - Business Logic
  - Models
  - Media Player Service

## Development Team

| NIM      | Contributions                                     | Hours |
|----------|--------------------------------------------------|-------|
| 13522136 | Notifications, Profile Edit, Songs Recommendations     | 20     |
| 13522150 | Audio Routing, Output Device, Share Songs(URL & QR) | 20    |
| 13522129 | Online Songs, Download Online Songs, Sound Capsule | 20    |






