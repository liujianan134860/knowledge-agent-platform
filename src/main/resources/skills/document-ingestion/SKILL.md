# Document Ingestion Skill

## Overview
This skill defines the process for uploading, parsing, and chunking documents into the knowledge base.

## Supported Formats
| Format | Extensions | Parser |
|--------|-----------|--------|
| PDF | `.pdf` | Apache PDFBox `PDFTextStripper` |
| Word | `.docx` | Apache POI `XWPFDocument` |
| Plain Text | `.txt` | UTF-8 text reader |
| Markdown | `.md` | UTF-8 text reader |

## Ingestion Flow
1. Receive file upload via `POST /api/documents/upload`
2. Validate file type and size (max 20MB)
3. Extract raw text using the appropriate parser
4. Normalize whitespace (collapse multiple spaces, limit consecutive newlines)
5. Split text into chunks:
   - First split on Markdown headings (`##`, `###`, etc.)
   - Then split by paragraph boundaries (`\n\n+`)
   - For long paragraphs, split by sentence boundaries (。！？.!?)
   - For very long sentences, split with character limit + overlap

## Chunking Configuration
- Default max chunk size: 1500 characters
- Overlap: max(80, maxChars / 10)
- Preserve Markdown heading context
- Preserve paragraph boundaries where possible

## Metadata
Each chunk should carry:
- `title` (document title + optional chunk index)
- `tags` (user-provided or default "upload")
- `createdAt` timestamp
- `id` (auto-incremented)

## Post-Ingestion
- Chunks are immediately available for search
- Refresh the document list in the UI to see new chunks
- Chunks support deletion by ID
