# SignalOne 🛡️
**Tu Primera Respuesta** — App de seguridad personal con botón de pánico para Android.

---

## Descripción

SignalOne es una aplicación Android nativa diseñada para situaciones de emergencia. Al presionar el botón de pánico, envía automáticamente un SMS y un mensaje de WhatsApp con el link de ubicación en Google Maps a todos los contactos de confianza registrados. Incluye modos de activación discretos para cuando no es posible usar la pantalla.

---

## Stack tecnológico

| Elemento | Versión |
|---|---|
| Lenguaje | Kotlin |
| UI | XML + Material Design 3 |
| Build System | Gradle 8.4 (KTS) |
| Android Gradle Plugin | 8.2.2 |
| Kotlin | 1.9.22 |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 34 (Android 14) |
| View Binding | Habilitado |

**Dependencias externas:**
- `com.google.android.gms:play-services-location:21.2.0` — GPS y FusedLocationProvider

---

## Estructura del proyecto

```
app/src/main/
├── java/com/signalone/app/ui/
│   ├── AppState.kt
│   ├── UserPreferences.kt
│   ├── WhatsAppHelper.kt
│   ├── PanicService.kt
│   ├── VolumeReceiver.kt
│   ├── BienvenidaActivity.kt
│   ├── RegistroActivity.kt
│   ├── LoginActivity.kt
│   ├── RecuperarActivity.kt
│   ├── PrincipalActivity.kt
│   ├── AlertaActivaActivity.kt
│   ├── ContactosActivity.kt
│   ├── HistorialActivity.kt
│   └── ModoDiscretoActivity.kt
├── res/
│   ├── drawable/          — íconos y fondos vectoriales
│   ├── layout/            — 9 pantallas + 2 items de lista
│   ├── mipmap-*/          — íconos del launcher (mdpi → xxxhdpi)
│   └── values/            — colores, strings, temas
└── AndroidManifest.xml
```

---

## Arquitectura

La app sigue un patrón simple de **Activities + Singleton de estado global**, sin ViewModel ni arquitectura MVVM, lo cual es adecuado para el alcance del proyecto.

### `AppState.kt`
Singleton que actúa como fuente de verdad en memoria durante la sesión activa. Contiene:
- Lista de `Contacto` (nombre, teléfono, color de avatar)
- Flags del modo discreto: `volumenActivo`, `agitarActivo`, `bloqueadoActivo`
- URL de última ubicación GPS
- Historial de alertas (`List<AlertaHistorial>`)
- Origen de la alerta actual

### `UserPreferences.kt`
Capa de persistencia usando `SharedPreferences`. Guarda y carga en JSON:
- Datos de cuenta (correo + contraseña hasheada)
- Contactos de confianza
- Historial de alertas (máximo 50 entradas)
- Configuración del modo discreto

Los datos persisten entre sesiones y se cargan al iniciar la app en `BienvenidaActivity`.

---

## Pantallas

### 1. BienvenidaActivity
Pantalla de splash/entrada. Carga todos los datos persistidos (contactos, historial, modo discreto) antes de continuar. Redirige a Registro o Login.

### 2. RegistroActivity
Formulario de creación de cuenta. Valida campos y guarda las credenciales en `UserPreferences`. Al registrarse, navega directamente a la pantalla principal.

### 3. LoginActivity
Valida correo y contraseña contra la cuenta guardada. Muestra mensajes específicos según el tipo de error (correo no encontrado, contraseña incorrecta, sin cuenta).

### 4. RecuperarActivity
Verifica que el correo ingresado coincida con el guardado y muestra confirmación. En producción se conectaría a un backend para envío de enlace de recuperación.

### 5. PrincipalActivity ⭐
Pantalla central de la app. Contiene:
- **Botón de pánico** (círculo rojo con escudo blanco) con animación de pulso
- **Detección de volumen ×5** mediante `onKeyDown` — activa alerta si `volumenActivo` está encendido
- **Detección de agite** mediante `SensorManager` y acelerómetro — activa alerta si `agitarActivo` está encendido (umbral: 12 m/s²)
- Solicita permiso de ubicación al abrir y verifica que el GPS del sistema esté activo
- Banner dinámico que muestra el estado del modo discreto
- Instrucción visible solo si alguna función discreta está activa

**Flujo al activar pánico:**
```
Verificar permiso GPS
    → Obtener lastLocation (FusedLocationProvider)
        → Si no hay: getCurrentLocation (timeout 5s)
    → Construir URL: https://maps.google.com/?q=LAT,LNG
    → Enviar SMS a todos los contactos (SmsManager)
    → Registrar en historial (AppState + UserPreferences)
    → Abrir AlertaActivaActivity
        → AlertaActivaActivity abre WhatsApp contacto por contacto (delay 2.5s entre cada uno)
```

### 6. AlertaActivaActivity
Pantalla de confirmación de alerta enviada. Muestra:
- Estado progresivo por contacto: `Enviando...` → `✓ SMS enviado` → `✓ SMS + WhatsApp enviado`
- Card de ubicación tappable que abre Google Maps
- Botón para cancelar y marcar como falsa alarma (se registra en historial en verde)

La apertura de WhatsApp se hace con `Intent.ACTION_VIEW` hacia `https://api.whatsapp.com/send?phone=...` con el mensaje ya codificado. Se abre un contacto cada 2.5 segundos para dar tiempo al sistema.

### 7. ContactosActivity
Lista de contactos de confianza con soporte para agregar, editar y eliminar mediante un `BaseAdapter` personalizado y diálogos de confirmación. Cada cambio se persiste inmediatamente en `UserPreferences`.

### 8. HistorialActivity
Muestra el historial de alertas activadas con: tipo de activación, hora, contactos notificados y link de ubicación tappable. Incluye botón para limpiar el historial completo. Las falsas alarmas aparecen en verde.

### 9. ModoDiscretoActivity
Tres switches independientes:
- **Volumen ×5** — activa la alerta al presionar volumen 5 veces en menos de 2 segundos
- **Agitar dispositivo** — activa la alerta al sacudir el teléfono con fuerza
- **Pantalla bloqueada** — inicia `PanicService` para monitoreo en segundo plano

Todos los estados se persisten al cambiar cada switch.

---

## Funciones en segundo plano

### `PanicService.kt`
`ForegroundService` que se activa cuando "Pantalla bloqueada" está encendido en Modo Discreto. Mantiene una notificación discreta persistente ("SignalOne activo") y monitorea el acelerómetro en background. Al detectar un agite, obtiene la ubicación, envía SMS + WhatsApp y lanza `AlertaActivaActivity` por encima del lock screen.

Requiere permisos:
- `FOREGROUND_SERVICE`
- `FOREGROUND_SERVICE_LOCATION`
- `ACCESS_BACKGROUND_LOCATION`

`AlertaActivaActivity` tiene `android:showWhenLocked="true"` y `android:turnScreenOn="true"` para aparecer con pantalla bloqueada.

### `VolumeReceiver.kt`
`BroadcastReceiver` que detecta cambios de volumen del sistema (`android.media.VOLUME_CHANGED_ACTION`) cuando la app está en background. Cuenta 5 pulsaciones en una ventana de 2 segundos y activa el pánico si `volumenActivo` está habilitado.

### `WhatsAppHelper.kt`
Objeto utilitario que envía mensajes a múltiples contactos de WhatsApp de forma secuencial, con un delay de 2.5 segundos entre cada uno. Usa `setPackage("com.whatsapp")` para abrir WhatsApp directamente sin el selector del sistema. Incluye fallback sin `setPackage` si WhatsApp no está instalado.

---

## Permisos requeridos

| Permiso | Uso |
|---|---|
| `ACCESS_FINE_LOCATION` | GPS de alta precisión para la alerta |
| `ACCESS_COARSE_LOCATION` | Fallback de ubicación por red |
| `ACCESS_BACKGROUND_LOCATION` | Ubicación cuando la pantalla está bloqueada |
| `SEND_SMS` | Envío automático de SMS de emergencia |
| `FOREGROUND_SERVICE` | PanicService en segundo plano |
| `FOREGROUND_SERVICE_LOCATION` | Acceso a GPS desde el servicio |
| `VIBRATE` | Vibración al activar alerta desde background |
| `POST_NOTIFICATIONS` | Notificación del servicio (Android 13+) |
| `USE_FULL_SCREEN_INTENT` | Abrir pantalla sobre el lock screen |

---

## Persistencia de datos

Todos los datos se almacenan en `SharedPreferences` bajo dos archivos:

| Archivo | Contenido |
|---|---|
| `signalone_user` | Nombre, correo, contraseña de la cuenta |
| `signalone_app` | Contactos (JSON), historial (JSON), switches del modo discreto |

Los datos persisten mientras la app esté instalada. Al desinstalar se eliminan.

---

## Íconos

El ícono de la app es un círculo rojo carmesí (`#B2123A`) con un escudo blanco centrado, generado en todas las densidades estándar:

| Carpeta | Tamaño |
|---|---|
| `mipmap-mdpi` | 48×48 px |
| `mipmap-hdpi` | 72×72 px |
| `mipmap-xhdpi` | 96×96 px |
| `mipmap-xxhdpi` | 144×144 px |
| `mipmap-xxxhdpi` | 192×192 px |
| `mipmap-anydpi-v26` | Ícono adaptativo (Android 8+) |

---

## Cómo correr el proyecto

1. Clona el repositorio
2. Abre la carpeta raíz en **Android Studio**
3. Sincroniza Gradle (`Sync Now`)
4. Conecta un dispositivo físico o usa el emulador
5. Presiona ▶️ **Run**

> Para probar las funciones de GPS y WhatsApp se recomienda dispositivo físico. En el emulador la ubicación por defecto es Mountain View, CA — se puede cambiar en Extended Controls → Location.

---

## Autores

- **July Tatiana Ariza Peña** — Desarrollo
- **Jorge Eliecer Montes Rodríguez** — Desarrollo

Universidad Antonio Nariño — Ingeniería de Software  
Construcción de Aplicaciones Móviles · 2026
