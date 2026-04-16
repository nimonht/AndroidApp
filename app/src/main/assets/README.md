# TFLite Model Assets

This directory contains TensorFlow Lite model files for on-device inference.

## Required Model

- **File:** `use_multilingual_lite.tflite`
- **Source:** [MediaPipe Universal Sentence Encoder](https://ai.google.dev/edge/mediapipe/solutions/text/text_embedder#universal_sentence_encoder)
- **License:** Apache 2.0
- **Size:** ~6 MB
- **Output:** 100-dimensional dense vectors
- **Input:** String text processed via MediaPipe TextEmbedder

### Download

Download the model directly from the MediaPipe Model Garden:

```
curl -L -o app/src/main/assets/use_multilingual_lite.tflite \
  "https://storage.googleapis.com/mediapipe-models/text_embedder/universal_sentence_encoder/float32/latest/universal_sentence_encoder.tflite"
```

### How It Is Used

- `ModelManager` copies the model from this assets directory to the app's
  internal files directory on first use (MediaPipe requires a `File` path).
- `TFLiteEmbeddingService` loads the model via MediaPipe `TextEmbedder` and
  produces 100-dim float vectors for search queries and quiz text.
- `EmbeddingIndexWorker` runs in the background to generate and persist
  embeddings for all quizzes into the Room database.
- `EmbeddingCache` loads persisted embeddings into memory for fast
  cosine-similarity lookups during hybrid search.
