package com.rishabh.rag.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rishabh.rag.model.Document;

public interface DocumentRepository extends JpaRepository<Document, UUID> {
}