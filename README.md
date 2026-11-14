# Galaxy for Logent

Logent에서 삼성 갤럭시 앱과 One UI 설정 화면을 여는 독립 `.lpg` 플러그인이에요.
공개 Intent·딥링크·`PackageManager`만 사용하고, 비공개 API·Knox SDK·서명 권한·
시스템 앱 데이터 접근은 쓰지 않아요.

- 저장소: <https://github.com/logentworld/logent-galaxy>
- 플러그인 id: `com.sleepysoong.logent.galaxy`
- 플러그인 key: `sleepysoong:galaxy`
- 엔트리: `com.sleepysoong.logent.galaxy.GalaxyPlugin`
- 공개 API: `com.sleepysoong.logent:logent-plugin-api` (`plugin-api.lock`에 고정)

`logent-plugin.json`을 `scripts/plugin-pack.py build --definition logent-plugin.json`의
공통 정의로 사용해요.

## 도구

모든 도구는 `WRITE`이고 `childVisible = false`예요. 화면을 열거나 편집 화면을 여는
동작만 하며, 값을 바꾸거나 전송·저장을 대신하지 않아요.

### `galaxy_apps` — 갤럭시 앱

| action | 필드 | 설명 |
|---|---|---|
| `open` | `app` | 설치된 후보 패키지의 launcher intent로 앱을 열어요. launcher activity가 없으면 삼성 스토어 → Play 스토어 상세 화면을 열어요. |
| `list` | - | 매니페스트에 선언한 삼성 패키지 중 설치된 앱의 라벨·패키지·버전을 보여 줘요. |
| `status` | - | 제조사·브랜드·모델·Android·보안 패치·One UI 추정 버전·보안 폴더·Knox 구성 요소 설치 여부를 보여 줘요. |

`app` 값은 `notes`, `reminder`, `messages`, `phone`, `contacts`, `calendar`, `gallery`,
`myfiles`, `health`, `wallet`, `smartthings`, `modes`, `securefolder`, `wearable`, `buds`,
`theme`, `members`, `store`, `browser`, `camera`, `clock`, `calculator`, `weather`, `bixby`,
`dex`, `bixbyroutines` 중 하나예요.

설치되지 않은 앱은 "열었어요"로 보고하지 않고, 설치되지 않았다는 오류와 스토어 설치
안내를 돌려줘요. 실행 activity가 없어 스토어 화면만 연 경우에도 앱 화면은 열리지
않았다고 명시해요.

### `galaxy_notes` — 삼성 노트

| action | 필드 | 설명 |
|---|---|---|
| `open` | - | 삼성 노트 앱을 열어요. |
| `create` | `title`, `body` | 공개 `Intent.ACTION_INSERT` + `vnd.android.cursor.dir/note`로 새 노트 편집 화면을 열어요. 제목은 `android.intent.extra.SUBJECT`, 본문은 `android.intent.extra.TEXT`로 전달해요. |
| `list` | - | 공개 API가 없어 지원하지 않는다는 오류를 돌려줘요. |

### `galaxy_reminder` — 삼성 리마인더

| action | 필드 | 설명 |
|---|---|---|
| `open` | - | 삼성 리마인더 앱을 열어요. |
| `create` | `title`, `body` | 공개 `Intent.ACTION_INSERT` + `vnd.android.cursor.dir/reminder`로 새 리마인더 편집 화면을 열어요. |
| `list` | - | 공개 API가 없어 지원하지 않는다는 오류를 돌려줘요. |

### `galaxy_messages` — 삼성 메시지

| action | 필드 | 설명 |
|---|---|---|
| `open` | - | 삼성 메시지 앱을 열어요. |
| `compose` | `to`, `body` | 공개 `Intent.ACTION_SENDTO` + `sms:<번호>`로 작성 화면을 열어요. 본문은 `sms_body` extra로 전달해요. |
| `search` | - | 확인된 공개 검색 딥링크가 없어 지원하지 않는다는 오류를 돌려줘요. |

문자를 전송하지 않고, 다른 앱의 문자 DB를 직접 읽지 않아요.

### `galaxy_settings` — 갤럭시 설정

| action | 필드 | 설명 |
|---|---|---|
| `open` | `section` | 삼성 설정 컴포넌트와 표준 Settings Intent를 순서대로 시도하고, 실패하면 `android.settings.SETTINGS`로 폴백해요. |

`section` 값은 `oneui_home`, `navigation`, `edge_panels`, `modes`, `secure_folder`,
`battery`, `display`, `sound`, `notifications`, `privacy`, `accounts`, `system_update`,
`about` 중 하나예요.

이 도구는 설정 **화면만** 열고 값을 바꾸지 않아요. 결과에도 그렇게 알려요.

### `galaxy_quick_actions` — 갤럭시 빠른 동작

| action | 설명 |
|---|---|
| `bixby` | 빅스비 앱을 열어요. |
| `smartthings` | 스마트싱스 앱을 열어요. |
| `modes` | 모드 및 루틴 앱을 열어요. |
| `dex_mode` | Samsung DeX 설정 화면을 열어요. DeX 전환·해제는 공개 API가 없어 화면에서 직접 선택해야 한다고 알려요. |

## 동작 원칙

1. 설치 여부는 `PackageManager.getPackageInfo`로, 실행 화면은
   `getLaunchIntentForPackage`로 확인하고 예외를 처리해요.
2. 앱 후보가 여러 개면 설치된 첫 후보를 사용해요.
3. 앱 실행은 공개 launcher intent만 써요. 후보가 설치되어 있지만 launcher activity가
   없으면 삼성 스토어 딥링크(`samsungapps://ProductDetail/<package>`)와
   Play 스토어(`market://details?id=<package>`) 순서로 상세 화면을 열어요.
4. 읽기·자동화가 공개 API로 불가능한 기능(노트·리마인더 목록, 메시지 검색, 모드 전환,
   DeX 전환)은 빈 성공이나 가짜 결과 대신 명확한 오류로 알려 줘요.
5. `ACTION_INSERT`·`ACTION_SENDTO`는 편집·작성 화면을 여는 것까지만 해요. 저장과 전송은
   사용자가 앱에서 직접 확인해야 해요.

## 한계

- 삼성 앱 버전과 One UI 버전에 따라 설정 컴포넌트 이름과 딥링크가 달라질 수 있어요.
  후보를 순서대로 시도하고 마지막에는 표준 설정 화면으로 폴백하지만, 기기에 따라
  원하는 하위 화면 대신 설정 첫 화면이 열릴 수 있어요.
- Android 11 이상의 패키지 공개 범위 때문에 `galaxy_apps list`는 매니페스트의
  `<queries>`에 선언한 삼성 패키지만 확인해요. `QUERY_ALL_PACKAGES` 권한은 쓰지 않아요.
- One UI 버전은 `Build.VERSION.SEM_PLATFORM_INT`가 있을 때만 추정해요. 제조사 확장
  필드라 없는 기기에서는 "확인하지 못했어요"로 표시해요.
- 보안 폴더·Knox는 설치 여부만 확인해요. 보안 폴더 내부 데이터나 Knox SDK 기능은
  다루지 않아요.
- 삼성 메시지의 `sms_body` extra를 무시하는 앱 버전에서는 본문이 작성 화면에 자동으로
  채워지지 않을 수 있어요.

## 매니페스트

런타임 권한을 요청하지 않아요. 설치 여부·실행 activity 확인을 위해 `<queries>`에 삼성
앱 패키지와 `INSERT`, `SENDTO sms:`, `VIEW samsungapps:`, `VIEW market:`,
`android.settings.SETTINGS` Intent를 선언해요.

## 설치

1. 릴리스에서 `.lpg`를 내려받아요.
2. Logent 설정 → 플러그인에서 `.lpg`를 설치하고 Galaxy를 켜요.
3. 모델에게 "삼성 노트 열어 줘", "원UI 홈 화면 설정 열어 줘"처럼 요청해요.

## 개발

```bash
# JVM 단위 테스트와 lint
./scripts/test.sh

# .lpg 플러그인 패키지 만들기
./scripts/build.sh -PlogentPluginApiMavenLocal=true
```

산출물은 `build/dist/logent-galaxy-v<version>.lpg` 하나예요.

테스트는 `GalaxyCatalogTest`, `GalaxyIntentsTest`, `GalaxyPluginTest`로 나뉘어요.
카탈로그의 패키지·섹션 중복과 빈 값, 설치 후보 선택, `ACTION_INSERT`·`ACTION_SENDTO`·
스토어 딥링크·설정 Intent 스펙, 도구 선언을 Android Context 없이 검증해요.
Android 프레임워크를 실행하는 계측 테스트는 만들지 않아요.

## 보안

- 비공개 API, Knox SDK, 서명 권한, 시스템 앱 데이터 직접 접근을 쓰지 않아요.
- 비밀값·토큰을 저장하거나 로그에 남기지 않아요.
- 앱이 돌려주는 화면과 사용자가 입력한 제목·본문은 신뢰할 수 없는 데이터예요. 모델은
  지시가 아니라 사용자 요청을 수행하는 값으로만 다뤄야 해요.

## 라이선스

GNU General Public License v3.0이에요. 전문은 `plugin/LICENSE`에 있어요.
출처와 의존성 안내는 `NOTICE`에 있어요.
