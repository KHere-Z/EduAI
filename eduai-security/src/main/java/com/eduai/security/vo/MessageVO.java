package com.eduai.security.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 站内消息 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageVO {

    private Long id;
    private String type;
    private String title;
    private String content;

    /** 是否已读 */
    private Boolean read;

    private LocalDateTime createdAt;
}
