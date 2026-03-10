# Zipup WebView SDK

Android용 WebView SDK로, 웹뷰와 네이티브 앱 간의 양방향 통신을 지원합니다.

## 📋 목차

- [빌드 방법](#빌드-방법)
- [AAR 파일 생성](#aar-파일-생성)
- [사용 방법](#사용-방법)
- [API 개요](#api-개요)

## 🔨 빌드 방법

> **참고**: 이 SDK는 단독으로 빌드 가능합니다. `zipup-sdk-android` 폴더만으로도 AAR 파일을 생성할 수 있습니다.

### 안드로이드 스튜디오에서 빌드하기

#### 1. 프로젝트 열기

1. Android Studio를 실행합니다
2. `File` → `Open`을 선택합니다
3. **`zipup-sdk-android` 폴더를 직접 선택**합니다 (단독 프로젝트로 열기)

#### 2. Gradle 동기화

프로젝트를 열면 자동으로 Gradle 동기화가 시작됩니다. 수동으로 동기화하려면:

- 상단 메뉴: `File` → `Sync Project with Gradle Files`
- 또는 툴바의 동기화 아이콘 클릭

#### 3. 빌드 Variant 선택

1. 좌측 하단의 `Build Variants` 탭을 클릭합니다
2. `zipup-sdk-android` 모듈의 `Active Build Variant`를 선택합니다:
   - **Debug**: 개발/테스트용
   - **Release**: 배포용 (최적화됨)

#### 4. 빌드 실행

**방법 1: 메뉴를 통한 빌드**

1. 상단 메뉴에서 `Build` → `Make Module 'zipup-sdk-android'` 선택
2. 또는 `Build` → `Rebuild Project` 선택 (전체 프로젝트 재빌드)

**방법 2: Gradle 패널을 통한 빌드**

1. 우측의 `Gradle` 패널을 엽니다
2. `zipup-sdk-android` → `Tasks` → `build` 확장
3. 다음 중 하나를 더블클릭:
   - `assemble`: Debug와 Release 모두 빌드
   - `assembleDebug`: Debug 버전만 빌드
   - `assembleRelease`: Release 버전만 빌드

**방법 3: 터미널을 통한 빌드**

프로젝트 루트(`zipup-sdk-android` 폴더)에서 다음 명령어 실행:

```bash
# Gradle Wrapper가 있는 경우
./gradlew assembleDebug
./gradlew assembleRelease
./gradlew assemble

# Gradle이 설치되어 있는 경우
gradle assembleDebug
gradle assembleRelease
gradle assemble
```

> **참고**: Gradle Wrapper가 없는 경우, Android Studio에서 프로젝트를 열면 자동으로 생성됩니다.

## 📦 AAR 파일 생성

빌드가 완료되면 AAR 파일이 자동으로 생성됩니다.

### AAR 파일 위치

```
zipup-sdk-android/build/outputs/aar/
├── my-webview-sdk-debug.aar      # Debug 버전
└── my-webview-sdk-release.aar    # Release 버전
```

### AAR 파일 확인 방법

1. Android Studio의 `Project` 뷰에서 `zipup-sdk-android` → `build` → `outputs` → `aar` 폴더로 이동
2. 또는 파일 탐색기에서 위 경로로 직접 이동

### AAR 파일 사용

생성된 AAR 파일을 다른 프로젝트에서 사용하려면:

1. 프로젝트의 `libs` 폴더에 AAR 파일을 복사합니다
2. `app/build.gradle.kts`에 의존성을 추가합니다:

```kotlin
dependencies {
    implementation(files("libs/my-webview-sdk-release.aar"))
}
```

## 🚀 사용 방법

### 기본 사용

```kotlin
// 1. SDK 초기화
val webViewManager = MyWebViewManager(context)

// 2. 이벤트 리스너 등록
webViewManager.setEventListener(object : SDKEventListener {
    override fun onEvent(event: String, data: String) {
        // 이벤트 처리 (data는 JSON 문자열)
    }
})

// 3. SDK 초기화 및 실행
webViewManager.init(SDKInitData(
    clientId = "your-client-id",
    clientSecret = "your-client-secret",
    targetUrl = "https://your-url.com"
))
webViewManager.launchWebViewActivity()
```

## 📚 API 개요

### MyWebViewManager

SDK의 메인 매니저 클래스입니다.

**주요 메서드:**

- `init(data: SDKInitData)`: SDK 초기화
- `setEventListener(listener: SDKEventListener)`: 이벤트 리스너 설정
- `launchWebViewActivity()`: 웹뷰 액티비티 실행

### SDKInitData

SDK 초기화에 필요한 데이터입니다.

```kotlin
data class SDKInitData(
    val clientId: String,
    val clientSecret: String,
)
```

### SDKEventListener

웹뷰에서 전달되는 이벤트를 받는 인터페이스입니다.

```kotlin
interface SDKEventListener {
    fun onEvent(event: String, data: String)
}
```

### 웹뷰에서 메시지 전송

웹뷰의 JavaScript에서 다음과 같이 호출할 수 있습니다:

```javascript
// 이벤트 전송
window.Bridge.postMessage("onSuccess", JSON.stringify({...}));

// 연결 테스트
window.Bridge.ping(); // "pong" 반환
```

## 🔧 빌드 설정

### 최소 SDK 버전

- **minSdk**: 24 (Android 7.0)

### 컴파일 SDK 버전

- **compileSdk**: 36

### Java 버전

- **Java 11** 호환

## 📝 참고사항

- Release 빌드는 ProGuard 최적화가 적용되지 않습니다 (`isMinifyEnabled = false`)
- AAR 파일은 빌드 완료 후 `build/outputs/aar/` 폴더에 생성됩니다
- 빌드 전에 Gradle 동기화가 완료되었는지 확인하세요
