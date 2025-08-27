# ING-DiBa Coding Challenge – Email Topic Analysis

<img width="2680" height="500" alt="image" src="https://github.com/user-attachments/assets/799f3a60-e012-4af9-a681-0e6661bb382d" />

This repository contains my solution for the **ING-DiBa Senior Data Scientist Coding Challenge**.  
The goal was to analyze and classify the topics of approximately **630 customer emails** by leveraging state-of-the-art **NLP techniques** and **visual analytics**.

---

## 🧠 Project Overview

For the challenge, I designed a complete **end-to-end NLP pipeline** capable of:
- Discovering **latent topics** within the email corpus.
- Labeling individual sentences and entire documents with **topics**, **sentiment**, and **emotions**.
- Providing an **interactive exploration platform** for visualizing results.

To achieve this, I combined BERTopic, transformer-based NLP models, and the Docker Unified UIMA Interface (DUUI) from the [Text Technology Lab](https://www.texttechnologylab.org), resulting in a powerful, modular, and reusable pipeline.

## ⚙️ NLP Pipeline with DUUI

The workflow for processing and analyzing the emails was as follows:

### 1. Train a BERTopic Model
- Created embeddings for all emails and trained a BERTopic model to identify clusters and underlying topics.  
- Experimented with hyperparameter tuning due to the small dataset size (~630 emails).  
- The trained model is published on Hugging Face: [**bertopic-german-mails-small**](https://huggingface.co/TheItCrOw/bertopic-german-mails-small)

> For details, see [`python/BERTopic.ipynb`](python/BERTopic.ipynb).

---

### 2. Transform Emails into UIMA Documents
- Converted raw emails into the **UIMA** format (*Unstructured Information Management Architecture*) for downstream NLP processing.

---

### 3. Define the NLP Pipeline
Using the [Docker Unified UIMA Interface](https://github.com/texttechnologylab/DockerUnifiedUIMAInterface), I built the following NLP pipeline:

- **spaCy** for tokenization, lemmatization, sentence splitting, dependency parsing, and Named-Entity Recognition (NER).
- The trained BERTopic model to label topics on a sentence level, with aggregated document-level classifications.

> Additionally, I thought it would be interesting in this context to do sentiment and emotion analysis. **What are the customers sentiment and emotions when talking about specific topics?**

- A [sentiment analysis component](https://github.com/texttechnologylab/duui-uima/tree/main/duui-transformers-sentiment-atomar) to classify positive, negative, or neutral tone per sentence.
- An [emotion detection component](https://github.com/texttechnologylab/duui-uima/tree/main/duui-transformers-Emotion) to capture fine-grained emotional signals.

All raw and processed UIMA documents are stored under the [`data`](data) directory.

---

## 🌐 Interactive Exploration with UCE

<p align="center">
  <img width="250" height="250" alt="image" src="https://github.com/user-attachments/assets/6aec71cf-5ee0-43d8-8308-fe544a13c46b" />
</p>

To make the analysis tangible and intuitive, I integrated the processed corpus into the [**Unified Corpus Explorer (UCE)**](https://github.com/texttechnologylab/UCE). This provides a fully featured web interface for navigating annotations, exploring topics, and visualizing patterns in the dataset.

## 🔍 In Medias Res

| | |
|---------|---------|
| <img alt="image" src="https://github.com/user-attachments/assets/a95881dd-1662-4e7b-a17d-cfc29c3875a8" style="max-width:100%;"> | <img alt="image" src="https://github.com/user-attachments/assets/f7d907dd-7cdc-4750-8a09-bd887198d942" style="max-width:100%;"> |
| **Search Portal** – Explore annotated emails with flexible full-text and semantic search | **Lexicon Overview** – Browse all annotations, entities, and topics |
| <img alt="image" src="https://github.com/user-attachments/assets/ebea3922-090e-4fba-8ec5-e4fc2237f788" style="max-width:100%;"> | <img alt="image" src="https://github.com/user-attachments/assets/8ea556fc-ac09-4575-876e-74284a72ad09" style="max-width:100%;"> |
| **Topic Insights** – A wiki-style overview of a topic and its distribution across all emails | **Corpus Topic Distribution** – Visual representation of topic proportions |
| <img alt="image" src="https://github.com/user-attachments/assets/fd8900d8-bd2f-455e-ba93-b9accdbaf4d8" style="max-width:100%;"> | <img alt="image" src="https://github.com/user-attachments/assets/21570d5e-a997-4fc5-ab63-345c6c41a94c" style="max-width:100%;"> |
| **Document Reader** – View individual emails with sentiment and emotion annotations highlighted | **Annotation Visualizations** – Rich data visualizations of extracted patterns |

---

## 📦 Tech Stack

- **Python** (BERTopic, Transformers, spaCy)
- **Docker DUUI** for modular NLP pipelines
- **Hugging Face** for model storage & inference
- **UCE** for visualization & data exploration

