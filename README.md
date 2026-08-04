<div align="center">

# 🗼 Pharos

### Offline-first attendance companion for EduPrime students

*Track. Verify. Stay Ahead.*

</div>

---

## 📖 About

**Pharos** is an offline-first Android application designed to simplify attendance tracking for **EduPrime** students.

Instead of constantly switching between timetables, notes, calculators, and the EduPrime portal, Pharos brings everything together into a single, privacy-focused experience.

It allows students to record attendance instantly, synchronize with EduPrime, verify attendance updates, analyze progress, and continue working even without an internet connection.

> **Pharos doesn't replace EduPrime.**
>
> It works alongside it to make attendance management faster, smarter, and more reliable.

---

## ✨ Highlights

- 📱 Offline-first architecture
- 🔄 Secure EduPrime synchronization
- ✅ Attendance verification
- 📅 Intelligent timetable management
- 📊 Attendance analytics
- 📖 Searchable attendance history
- 🔐 Encrypted credential storage
- 👆 Biometric credential protection
- 🌙 Material 3 Dynamic Theme
- 💾 Backup & Restore
- 🚀 Background synchronization
- 📌 Daily attendance snapshots

---

# 📸 Screenshots

| Dashboard | History | Statistics |
|-----------|----------|------------|
| *(Coming Soon)* | *(Coming Soon)* | *(Coming Soon)* |

| Timetable | Sync | Settings |
|------------|------|----------|
| *(Coming Soon)* | *(Coming Soon)* | *(Coming Soon)* |

---

# 🚀 Features

## Dashboard

The Dashboard provides an overview of the current academic day.

- Daily timetable
- NOW / NEXT indicators
- Quick attendance marking
- Weekly preview
- Attendance states
- First-time onboarding

---

## Attendance History

A complete chronological log of attendance records.

Features include:

- Timeline view
- Subject search
- Status filters
- Verification badges
- Daily grouping

---

## Statistics

Track attendance progress with:

- Overall attendance
- Goal tracking
- Subject breakdown
- Attendance percentages
- Safety indicators

---

## Synchronization

Pharos securely synchronizes attendance with EduPrime.

Supports:

- Manual synchronization
- Background synchronization
- Attendance verification
- Daily attendance snapshots
- Sync history

---

## Timetable

Create and manage your timetable with:

- Subject management
- Working day configuration
- Import / Export
- Backup restoration

---

## Security

Your credentials remain protected through multiple security layers.

- EncryptedSharedPreferences
- Android Biometric Authentication
- Automatic credential masking
- Secure local storage

---

# 🏗 Architecture

```text
                Pharos

        Jetpack Compose UI
                │
                ▼
           ViewModels (MVVM)
                │
                ▼
           Repository Layer
        ┌────────┴────────┐
        ▼                 ▼
   Room Database     EduPrime Portal
        │
        ▼
   WorkManager Sync
```

---

# 🛠 Tech Stack

| Layer | Technology |
|--------|------------|
| Language | Kotlin |
| UI | Jetpack Compose |
| Architecture | MVVM |
| Database | Room |
| Async | Kotlin Coroutines |
| Reactive State | StateFlow |
| Background Tasks | WorkManager |
| Security | EncryptedSharedPreferences |
| Authentication | AndroidX Biometric |
| Design | Material 3 |

---

# 🎯 Design Principles

Pharos was built around a few simple principles.

- Offline First
- Privacy First
- Minimal User Effort
- Reliable Synchronization
- Clean Material Design
- Student-Centered Experience

---

# 🔒 Privacy

Pharos stores attendance information locally on your device.

Sensitive credentials are encrypted using Android's secure storage mechanisms.

Biometric authentication is required before revealing stored credentials.

No unnecessary personal data is collected.

---

# 🗺 Roadmap

## ✅ Version 1.0

- Offline attendance tracking
- EduPrime synchronization
- Verification engine
- Statistics
- History
- Timetable
- Backup & Restore
- Material 3 UI
- Biometric protection

---

## 🔜 Version 1.1

Planned improvements include:

- Enhanced daily attendance insights
- Smarter synchronization summaries
- Additional analytics
- Performance optimizations

---

# 🤝 Contributing

Pharos is currently a personal project.

Suggestions, bug reports, and constructive feedback are always welcome.

---

# 📄 License

This project is licensed under the MIT License.

---

<div align="center">

## Built with ❤️ by Rizen

**Pharos v1.0**

*"A lighthouse doesn't move ships.*

*It helps them reach their destination."*

</div>
