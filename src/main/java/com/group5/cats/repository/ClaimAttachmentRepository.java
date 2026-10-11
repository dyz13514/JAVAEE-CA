package com.group5.cats.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.group5.cats.model.ClaimAttachment;

public interface ClaimAttachmentRepository extends JpaRepository<ClaimAttachment, Long> {
}
