# 🔐 DoubleEncrypt512  
*Because if your encryption isn't overkill, is it even encryption?*

Welcome to **DoubleEncrypt512**, the only app that treats your grocery lists like top-secret nuclear codes.  
We’ve combined the nostalgia of Android Holo Blue with the "don't-mess-with-me" power of **512-bit cascading entropy**.

---

## 🧐 What is this?

A high-security file vault for Android that utilizes **Three-Factor Physical Binding**:

### 🔑 Three-Factor Physical Binding

- **Factor 1 — Hardware-backed Keystore**  
  Uses your phone’s internal TEE (Trusted Execution Environment) for key isolation.

- **Factor 2 — Biometric Signature**  
  Requires a mandatory fingerprint scan to unlock the vault in RAM.

- **Factor 3 — Physical NFC Tag**  
  A Mifare / ISO NFC tag acting as the physical "pointer" to your 512-bit master key.

### 🧠 The Result

Your files are saved as `.vlt` blobs.

- If a hacker steals your phone → they have nothing.  
- If they steal your NFC tag → they have a piece of plastic.  
- They need **THE PHONE + THE TAG + YOUR FINGER** to see your secret memes.

---

## 🚀 Features

- 🚫 **100% Ad-Free** — No banners, no pop-ups, no trackers. Just raw code.  
- 🔵 **Holo UI** — Clean, dark, blue. When Android still felt like a power tool.  
- 🔋 **True 512-bit Cascade** — Two independent 256-bit AES-GCM layers. If one fails, the other stands firm.  
- 🧬 **Biometric Locking** — The master key lives inside the Secure Element and only unlocks after biometric verification.  
- 🏷️ **Physical Keycard** — Use your office badge, bus card, or any NFC sticker as your master key.

---

## 🔓 Is this Open Source?

**YES.**  
After intensive polishing and security hardening, the source code is public. Use it wisely.

---

## 🛠 How to Use

| Phase         | Action                                                     | Outcome                                       |
|---------------|-------------------------------------------------------------|-----------------------------------------------|
| Initiation    | Open the app, tap your NFC tag, scan your finger           | Binds the 512-bit key to your hardware        |
| Lockdown      | Select a file, tap your tag, scan your finger              | Generates encrypted `.vlt` file               |
| Rescue        | Select `.vlt`, tap same tag, verify identity               | Restores original file                        |

---

## 🤝 Community & Support

- **Issues** — Found a bug? Open a GitHub issue. Big-brain ideas welcome.  
- **Testers** — If you own weird NFC tags (hotel keys, bus cards, fossils), please test and report compatibility.

---

## ⚖️ Licensing

GPL Licensed.  
You are free to use, modify, and share the code, provided you credit the original author (**TheLazyCoder**).

---

## ⚠️ Warning

- **Don't Lose Your Tag** — Lose it = lose your data. Forever.  
- **Dog Danger** — If your dog eats your NFC keycard, your files are gone. Please protect your cryptographic snacks.
