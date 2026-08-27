# DateCourseRecommendApplication

지도 기반으로 데이트 코스를 기록하고 공유하는 Android 애플리케이션입니다. 위치와 사진을 담은 게시글을 지도 위에서 탐색하고, 마음에 드는 코스를 저장하거나 리트윗할 수 있습니다.

## 주요 기능

- **로그인**: 구글 계정 로그인 (Firebase Authentication 연동)
- **게시글 작성/편집**: 위치, 사진과 함께 데이트 코스 게시글 작성
- **지도 기반 탐색**: Kakao Maps로 게시글을 지도 위에 표시, 위치 검색(Kakao 로컬 검색 API)
- **소셜 기능**: 게시글 리트윗, 좋아요, 댓글
- **마이페이지**: 내가 쓴 글 / 좋아요한 글 모아보기, 프로필 편집
- **인앱 결제**: Google Play Billing 연동

## 기술 스택

- **언어**: Java (Kotlin 일부)
- **UI**: Jetpack Compose, XML(View) 혼용
- **백엔드**: Firebase (Authentication, Firestore, Storage, Cloud Functions, Analytics)
- **지도**: Kakao Maps SDK, Kakao Local Search API
- **네트워킹**: Retrofit2 + OkHttp
- **이미지 처리**: Glide
- **결제**: Google Play Billing Library

## 시작하기

### 요구 사항

- Android Studio (최신 버전 권장)
- minSdk 24 / targetSdk 36 / compileSdk 36
- JDK 11

### 빌드 설정

이 프로젝트는 API 키 등 민감 정보를 저장소에 포함하지 않습니다. 빌드 전 아래 파일을 직접 준비해야 합니다.

1. **`local.properties`** (프로젝트 루트) 에 다음 키를 추가하세요.

   ```properties
   kakao_native_app_key=YOUR_KAKAO_NATIVE_APP_KEY
   kakao_rest_api_key=YOUR_KAKAO_REST_API_KEY
   ```

2. **`app/google-services.json`**: Firebase 콘솔에서 프로젝트를 생성한 뒤 다운로드하여 `app/` 디렉터리에 위치시키세요.

두 파일 모두 `.gitignore`에 포함되어 있어 커밋되지 않습니다.

### 빌드 및 실행

```bash
./gradlew assembleDebug
```

Android Studio에서 프로젝트를 열고 실행(Run)해도 됩니다.

## 프로젝트 구조

```
app/src/main/java/com/najunho/datecourserecommendapplication/
├── Activity/       # 화면 단위 Activity (게시글, 유저, 지도, 결제 등)
├── CloudFunction/  # Firebase Cloud Functions 호출
├── DB/             # Firestore 레포지토리 (Post, User, Comment, Content)
├── RecycerView/    # RecyclerView 어댑터
├── Retrofit/       # Kakao API 네트워크 클라이언트
└── Util/           # 유틸리티(초기화, 키 관리 등)
```
