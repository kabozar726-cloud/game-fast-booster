# Game Fast Booster — Real Android App (source code)

**Ye asli Android app ka code hai** — web page nahi. Phone par VPN
sach mein ON karne ke liye yehi code chahiye, kyunki sirf native app
Android ke VpnService se VPN permission le sakti hai.

## Is code mein kya hai

- `MainActivity.kt` — server chunein, Connect/Disconnect, network test,
  Before/After ms log (player ke apne numbers, kabhi banawati nahi).
- `GameVpnService.kt` — asli `VpnService` + WireGuard tunnel backend.
  Pehli dafa Connect par Android ka system VPN dialog khud aayega.
- `ServerConfig.kt` — UAE / Singapore / Germany servers (endpoint aur
  public key ki jagah placeholder hai — asli server se badlein).
- `BoostLog.kt` — Before/After log, phone par hi mehfooz.
- `server-setup.md` — WireGuard server lagane ke poore steps.
- Play Billing ke liye product id: `gamefast_pro_monthly` (Rs 200/month).

## Ye code kya NAHI karta (sach)

- PUBG game client ko nahi chherta, anti-cheat ko bypass nahi karta.
  Aisa karne wali app account ban kara sakti hai — is liye ye "Mod"
  nahi, "Booster" hai.
- 20 ms ki guarantee nahi deta. Asli ping internet, server ki doori
  aur route par depend karta hai. App ka kaam: sab se tez route dena
  aur player ke apne numbers dikhana.

## APK kaise banegi (build steps)

Is machine par Android SDK maujood nahi tha, is liye APK yahan
compile nahi hui — code mukammal likh kar diya gaya hai. Build ke liye:

1. Android Studio (Hedgehog ya naya) mein ye folder kholein.
2. Gradle sync hone dein. Agar WireGuard library ka version na mile to
   https://github.com/WireGuard/wireguard-android se latest tunnel
   artifact sync karein (app/build.gradle mein likha hai).
3. `ServerConfig.kt` mein asli server endpoint + public key daalein.
4. `GameVpnService.kt` mein per-user keypair provisioning mukammal
   karein (server-setup.md step 4) — baghair is ke Connect error dega,
   jo jaan boojh kar rakha gaya hai taake shared private key ka
   security masla na ho.
5. Debug APK phone par install karein, Connect dabayein, Android ka
   VPN dialog allow karein, phir PUBG lobby mein ms check karein.

## Launch ke liye baqi kaam (kiska kaam hai)

- **Aap ka:** Play Console account ($25, aik dafa) + server kiraya
  (pehle 3 servers) + baad mein Easypaisa/JazzCash merchant account
  (sirf website payment ke liye; Play app ke andar Google Play Billing
  lazmi hai).
- **Hamara (agla marhala):** per-user key provisioning backend,
  Play Billing ki mukammal wiring, privacy policy + Data Safety form,
  Play Store listing, aur beta test (aapki apni game ka ms pehle/baar).

## Status (imaan dari se)

- Code: mukammal scaffold, Android Studio mein kholne ke liye tayyar.
- Compile/APK: is machine par SDK na hone ki wajah se yahan test
  nahi hui — Android Studio par build karte waqt chhoti dependency
  adjustment ki zaroorat par sakti hai.
- Server: abhi khareeda/setup nahi hua — us ke baghair Connect
  kaam nahi karega. Yehi agla marhala hai.
