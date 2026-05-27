# Smaran — AI-Powered Voice Memory Companion

> *"ChatGPT bhool jaata hai. Smaran yaad rakhta hai."*

[![Hackathon](https://img.shields.io/badge/Rumik%20×%20AWS-Voice%20Hackathon%202026-7C6AF7?style=flat-square)](https://rumik.ai)
[![Platform](https://img.shields.io/badge/Platform-Android-3AB87A?style=flat-square)](https://android.com)
[![Backend](https://img.shields.io/badge/Backend-Node.js-F7A85A?style=flat-square)](https://nodejs.org)
[![TTS](https://img.shields.io/badge/TTS-Silk%20by%20Rumik-F75A5A?style=flat-square)](https://rumik.ai)

---

## What Is Smaran?

**Smaran** (Sanskrit: *स्मरण* — remembrance) is an AI-powered personal voice memory companion built natively for Hinglish speakers.

You speak. It remembers. Later you ask anything about your life — it doesn't return a list. It tells you a **story**, narrated back to you in an emotionally aware voice.

This is not a chatbot. ChatGPT is a brilliant stranger you meet for the first time every single day. **Smaran is the one friend who actually remembers your life.**

---

## Built At

**Rumik × AWS Voice Hackathon**
The hackathon theme was: *Build a voice agent. STT → LLM → TTS. One day. Ship something that talks back.*

We went beyond that.

---

## The Problem

Every day you have conversations, make decisions, feel things. Within a week — 90% is forgotten. Not because you're careless. Because no tool exists that captures life the way Indians actually live it — speaking Hinglish, switching languages mid-sentence, moving fast.

| Solution | Problem |
|---|---|
| Notes apps | Too much friction. Nobody opens a notes app mid-conversation. |
| Voice memos | You have to listen back. Nobody does. |
| ChatGPT | Forgets you the moment you close the tab. |
| Rewind / Limitless | English only. US market. $99+. Not built for India. |

**600 million Hinglish speakers. Zero tools built for how they actually think and talk.**

---

## Demo Flow

```
User speaks in Hinglish (Android mic)
            ↓
    Deepgram/Google STT → raw transcript
            ↓
    Review + edit transcript on Android
            ↓
    POST /api/memory
            ↓
    Backend: Gemini extracts tags + generates vector embedding
    Background: connection detection across last 30 memories
            ↓
    Stored in PostgreSQL + pgvector
            ↓
    User asks: "Rahul ke saath is mahine kaisa raha?"
            ↓
    POST /api/memory/ask
            ↓
    Backend: embed query → vector search → tag re-rank
             → fetch connections → Gemini generates insight
             → mood detection → voice description generation
            ↓
    Response: { answer, mood, voiceDescription, speaker, pitchShift }
            ↓
    Android calls Silk Mulberry/Muga TTS directly
            ↓
    Answer narrated in emotionally matching voice
```

---

## What Makes It Different

### 1. Hybrid RAG Architecture
Not just keyword search. Not just vector search. Both combined.

- **Vector embeddings** (3072-dim via `gemini-embedding-001`) capture the *meaning* of every memory
- **Tag matching** boosts precision on top of semantic results
- Result: finds memories even when exact words differ

```
Query: "Woh khushi wala din kab tha?"
Tag search: no match for "khushi" → ❌
Vector search: finds "farewell party, maza aaya" → ✅ (meaning is close)
```

### 2. Emotional Arc Detection
Every memory saved triggers a background connection detection job. Gemini analyzes patterns across the last 30 memories and detects:

- Same person appearing repeatedly
- Emotional escalation or resolution over time
- Topic continuations across days/weeks
- Story arcs (conflict → resolution)

These connections get surfaced when you ask relationship or pattern questions.

### 3. Emotion-Aware TTS via Silk
Not just `[happy]` prefix on text. The backend generates a full dynamic voice description per answer:

```json
{
  "answer": "Is hafte Rahul ke saath teen interactions hue...",
  "mood": "sad",
  "voiceDescription": "warm close friend, gentle and concerned",
  "speaker": "speaker_1",
  "pitchShift": -1
}
```

Android calls **Silk Mulberry** directly with this metadata. The voice changes emotional tone as the story progresses — sad when describing stress, warm when describing resolution.

### 4. Native Hinglish Support
Built from the ground up for how Indians actually talk. Deepgram `nova-3` handles code-switching. Gemini prompts enforce Hinglish output in Roman script. Silk narrates naturally in Hinglish.

---

## Tech Stack

### Android (Ishan)
| Component | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose |
| Architecture | MVVM — ViewModel + StateFlow + SharedFlow |
| Navigation | Jetpack Navigation Compose |
| STT | Deepgram REST API (`nova-3` model) |
| TTS | Silk by Rumik (Mulberry model) |
| HTTP | Retrofit + OkHttp |
| Audio | Android AudioRecord → WAV |

### Backend (Naman)
| Component | Technology |
|---|---|
| Runtime | Node.js |
| Framework | Express.js |
| Database | PostgreSQL + pgvector |
| Auth | JWT + bcrypt |
| Hosting | Railway.app |

### AI / APIs
| Purpose | Model |
|---|---|
| Tag Extraction | Gemini 2.5 Flash |
| Vector Embedding | gemini-embedding-001 (3072 dimensions) |
| Answer Generation | Gemini 2.5 Flash |
| Connection Detection | Gemini 2.5 Flash |
| Voice Description | Gemini 2.5 Flash (same call as answer) |
| STT | Deepgram nova-3 |
| TTS | Silk Mulberry by Rumik |

---

## System Architecture

### Memory Save Pipeline

```
Android POST /api/memory { text, recordedAt, durationSeconds }
    │
    ├── Gemini 2.5 Flash → extractTags()
    │   Returns: { people[], topics[], mood, location[], summary }
    │
    ├── gemini-embedding-001 → generateEmbedding()
    │   Returns: vector[3072] representing semantic meaning
    │
    ├── PostgreSQL INSERT
    │   Stores: raw_text, tags, embedding, date_label, timestamps
    │
    └── Background: detectConnections()
        Gemini analyzes last 30 memories
        Finds: same_person | topic_continuation | emotional_pattern | story_arc
        Stores in: memory_connections table
```

### Memory Query Pipeline

```
Android POST /api/memory/ask { query }
    │
    ├── gemini-embedding-001 → embed query
    │
    ├── pgvector cosine similarity search
    │   SELECT * FROM memories ORDER BY embedding <=> queryVec LIMIT 20
    │
    ├── Gemini extractTags() on query
    │   Re-rank top 20 by tag overlap:
    │   people match +0.2 | topics match +0.15 | mood match +0.1
    │   Take top 5
    │
    ├── Fetch memory_connections for top 5
    │
    └── Gemini generateAnswer()
        Input: query + top 5 memories + connections
        Output: { answer, mood, voiceDescription, speaker, pitchShift }
```

### Database Schema

```sql
-- Core memory table
CREATE TABLE memories (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          TEXT NOT NULL,
    raw_text         TEXT NOT NULL,
    summary          TEXT,
    people           TEXT[],
    topics           TEXT[],
    mood             TEXT,
    location         TEXT[],
    date_label       TEXT,           -- DD-MM-YYYY
    recorded_at      TIMESTAMPTZ,
    duration_seconds INT,
    embedding        vector(3072),    -- semantic meaning
    created_at       TIMESTAMPTZ DEFAULT NOW()
);

-- Pattern connections between memories
CREATE TABLE memory_connections (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    memory_a        UUID REFERENCES memories(id),
    memory_b        UUID REFERENCES memories(id),
    connection_type TEXT,   -- same_person | topic_continuation | emotional_pattern | story_arc
    pattern         TEXT,
    insight         TEXT,
    created_at      TIMESTAMPTZ DEFAULT NOW()
);

-- Indexes
CREATE INDEX ON memories USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
CREATE INDEX ON memories USING GIN(people);
CREATE INDEX ON memories USING GIN(topics);
```

---

## API Endpoints

### Auth
```
POST /api/auth/signup    → Register, returns JWT
POST /api/auth/login     → Login, returns JWT
GET  /api/auth/profile   → Get profile
```

### Memory (All require Bearer token)
```
POST   /api/memory           → Save memory
POST   /api/memory/ask       → Query memories, get AI answer
GET    /api/memory/all       → List all memories
GET    /api/memory/:id       → Get specific memory
DELETE /api/memory/:id       → Delete memory
```

### Request / Response

**POST /api/memory**
```json
// Request
{ "text": "Aaj Rahul se baat ki, placement ke baare mein nervous tha", "recordedAt": "2026-05-24T09:41:00Z", "durationSeconds": 45 }

// Response
{ "id": "uuid", "status": "saved" }
```

**POST /api/memory/ask**
```json
// Request
{ "query": "Rahul ke saath is mahine kaisa raha?" }

// Response
{
  "answer": "Is mahine Rahul ke saath teen interactions hue. Shuruwat mein placement ka tension tha...",
  "mood": "sad",
  "voiceDescription": "warm close friend, gentle and concerned",
  "speaker": "speaker_1",
  "pitchShift": -1
}
```

---

## Android Architecture

```
com.example.smaran/
├── ui/
│   ├── record/
│   │   ├── RecordScreen.kt        ← Compose UI
│   │   └── RecordViewModel.kt     ← StateFlow + SharedFlow
│   ├── review/
│   │   ├── ReviewScreen.kt        ← Editable transcript
│   │   └── ReviewViewModel.kt
│   ├── ask/
│   │   ├── AskScreen.kt           ← Voice input + answer display
│   │   └── AskViewModel.kt
│   └── theme/
│       └── SmaranTheme.kt         ← Colors, typography, design tokens
├── data/
│   ├── api/
│   │   ├── SmaranApiService.kt    ← Retrofit interface
│   │   ├── DeepgramApiService.kt  ← STT calls
│   │   └── RetrofitClient.kt      ← OkHttp singleton
│   └── repository/
│       └── MemoryRepository.kt    ← All network calls
├── navigation/
│   ├── Screen.kt                  ← Route definitions
│   └── SmaranNavGraph.kt          ← NavHost
└── Constants.kt                   ← Base URLs, API keys
```

### State Management
- `StateFlow` — UI state (what the screen shows right now)
- `SharedFlow` — one-time navigation events (navigate after transcription, navigate after send)
- ViewModels survive recomposition, Fragments don't exist — Single Activity, all Compose

---

## UI / UX Design

### Design Language
Dark, minimal, techy. Feels like a personal OS, not a notes app. No gradients, no illustrations. Geometry, monospace type, purposeful color.

### Color Palette
```
Background:      #0D0D14
Surface:         #10101A
Surface Variant: #14141F
Border:          #1E1E2E
Purple:          #7C6AF7   ← primary accent
Red:             #F75A5A   ← recording active
Green:           #3AB87A   ← success states
Amber:           #F7A85A   ← processing states
Text Primary:    #D0D0E8
Text Muted:      #3A3A5A
```

### Typography
- **Space Mono** — all labels, tags, timestamps, buttons. Always uppercase, letter-spaced.
- **DM Sans** — body text only. Transcriptions, answers, memory content.

### Screens
- **Record Screen** — waveform visualizer, pulse ring animation, live timer, state-aware button
- **Review Screen** — editable transcript, word count, recording info chips, send button with state transitions
- **Ask Screen** — voice input + text input, Q&A history, answer cards with Silk play button
- **Profile Screen** — stats, STT/TTS toggle, language preference

> **Note on design:** UI visual designs and component layouts were fully generated using AI (Claude). The complete engineering specification behind the designs — color system, typography scale, component behavior, interaction patterns, animation logic, state management approach, and screen architecture — was conceived, directed, and refined by me. Every design decision has an engineering reason behind it.

---

## Key Engineering Concepts

### Why Vector Embeddings?
Tag matching fails on semantic queries. "Woh khushi wala din" won't match a memory tagged `farewell, excited` using keyword search. Vector embeddings convert text meaning into 768 numbers — semantically similar sentences have numerically close vectors. Cosine similarity finds them.

### Why Hybrid (Vector + Tags)?
- Vector alone: too broad. "Rahul ke baare mein batao" finds all Rahul memories including irrelevant ones.
- Tags alone: too brittle. Misses synonyms, paraphrases, Hinglish variations.
- Hybrid: vector casts a broad semantic net, tags re-rank for precision.

### Why Memory Connections?
Individual memories are isolated facts. Connections turn them into narrative. The pattern "Rahul appears 4 times, mood shifts from stressed to happy" is an insight no single memory contains. Background connection detection after every save makes this possible without blocking the user.

### Why Async Connection Detection?
Connection detection is a Gemini call over 30 memories — slow. Android should never wait for it. Fire and forget after save response. User gets instant confirmation, connections are built in background.

### Why Silk Mulberry Over Muga?
Muga supports global tone tags `[happy]` `[sad]` — simple but flat. Mulberry supports natural language voice descriptions, preset speakers, and pitch shifting. Dynamic voice description generated by Gemini per answer makes narration feel genuinely human — not TTS-robotic.

---

## The AI-Acceleration Note

This project was built in **one hackathon day (8 hours)**. The development speed was significantly accelerated through extensive use of AI tools — primarily Claude — for:

- Architecture design and technical decision making
- Boilerplate code generation (Kotlin, Node.js)
- Prompt engineering for all Gemini calls
- UI component scaffolding
- README and documentation

However, every core concept in this project — hybrid RAG architecture, vector embedding pipeline, emotional arc detection, memory connection system, emotion-aware TTS, Hinglish-first design — was **conceived, understood, directed, and engineered by the team.** AI was the tool. The engineering thinking was ours.

---

## Team

| Role | Person | Responsibility |
|---|---|---|
| Android Engineer | Ishan | Kotlin + Compose UI, Deepgram STT integration, Silk TTS integration, Android architecture, State management |
| Backend Engineer | Naman | Node.js + Express, PostgreSQL + pgvector, Gemini integration, RAG pipeline, Auth system, Railway deployment |

---

## Future Scope

### Immediate
- Daily summary: *"Aaj ka din kaisa tha"* — AI-generated evening recap
- Proactive surfacing: *"3 din pehle doctor appointment ka zikr kiya tha, koi update?"*
- Relationship timeline: visual arc of how dynamics with specific people evolved

### 3 Months
- Wearable hardware — small clip device, always-on passive recording, Bluetooth to phone, under ₹2000
- When that ships — Smaran becomes truly always-on. No tap required. Just live your life.

### The Vision
Right now you choose to record. The real unlock is when Smaran just listens — passively, privately, always — and you never lose a moment again.

---

## Hackathon Context

Built for **Rumik × AWS Voice Hackathon** — the theme was STT → LLM → TTS in one day.

**APIs provided at hackathon:**
- Silk by Rumik — TTS (used: Mulberry model with dynamic voice descriptions)
- Deepgram — STT (used: nova-3 model, REST API)
- Gemini — LLM (used: 2.5 Flash for tags, answers, connections, voice description)

We used all three. And built a persistent memory layer on top that none of the other teams had.

---

## Environment Variables

```env
# Backend (.env)
DATABASE_URL=postgresql://...
GOOGLE_API_KEY=your_gemini_key
JWT_SECRET=your_jwt_secret
PORT=3000
```

```kotlin
// Android (Constants.kt)
const val BASE_URL = "https://your-backend.railway.app/"
const val DEEPGRAM_API_KEY = "your_deepgram_key"
const val SILK_API_KEY = "your_silk_key"
```

---

## Running Locally

### Backend
```bash
git clone https://github.com/yourusername/smaran-backend
cd smaran-backend
npm install
# Add .env file
node index.js
```

### Android
```bash
git clone https://github.com/yourusername/smaran-android
# Open in Android Studio
# Add API keys to Constants.kt
# Run on device or emulator (API 24+)
```

---

## The One Line

> *We gave AI a memory. Not of facts — of your life.*

---

*Smaran — because you shouldn't have to remember everything yourself.*
