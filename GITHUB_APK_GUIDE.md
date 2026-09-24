# GitHub se APK Kaise Banaye (GitHub Actions Guide)

Aapke project me **GitHub Actions Workflow** configure kar diya gaya hai (`.github/workflows/build-apk.yml`). Ab jab bhi aap code GitHub par push karenge ya GitHub me **"Run workflow"** button dabayenge, GitHub automatically APK build karke download ke liye provide karega!

---

## 🚀 Step-by-Step Guide: APK Download Kaise Kare

### Option 1: AI Studio se GitHub par Push karein
1. AI Studio ke top-right corner me **Export / Settings** me jayein.
2. **"Push to GitHub"** ya **"Export to GitHub"** select karein.
3. Apna GitHub repository choose karein aur push karein.

---

### Option 2: GitHub Actions se Direct APK Build karein
Jaise hi repository GitHub par aayegi:
1. Apne GitHub repository me jayein (browser me open karein).
2. Upar menu me **`Actions`** tab par click karein.
3. Left sidebar me **`Build Android APK (MYRA AI)`** workflow select karein.
4. Right side me **`Run workflow`** dropdown button par click karein aur **"Run workflow"** click karein.
5. GitHub Actions 2-3 minute me poora Android APK build kar dega:
   - ✅ **`MYRA_AI_Debug.apk`**
   - ✅ **`MYRA_AI_Release.apk`**

---

### 📥 APK Download Kaise Karein
1. **Actions** tab me completed build (Green checkmark `✓`) par click karein.
2. Niche scroll karke **`Artifacts`** section me jayein.
3. Waha **`MYRA-AI-APKs`** par click karein — APK zip file download ho jayegi!
4. Unzip karke apne Android phone me install karein!

---

### 🔑 (Optional) Gemini API Key GitHub Secrets me add karein
Agar aap AI ke smart natural-language responses enable karna chahte hain:
1. GitHub Repository -> **Settings** -> **Secrets and variables** -> **Actions**
2. **New repository secret** par click karein:
   - Name: `GEMINI_API_KEY`
   - Value: `<Aapki Gemini API Key>`
3. Ab jab bhi APK build hoga, Gemini API automatic inject ho jayegi!

---

### 🏷️ Releases me Direct APK Chahiye?
Agar aap chahte hain ki GitHub Releases me APK directly available ho:
Git tag push karein:
```bash
git tag v1.0.0
git push origin v1.0.0
```
GitHub Actions automatically **Releases** page par APK upload kar dega!
