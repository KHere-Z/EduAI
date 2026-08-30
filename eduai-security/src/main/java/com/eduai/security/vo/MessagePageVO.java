package com.eduai.security.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 站内消息分页 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessagePageVO {

    private long total;
    private List<MessageVO> list;
}
