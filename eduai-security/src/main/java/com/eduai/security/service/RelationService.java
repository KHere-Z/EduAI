package com.eduai.security.service;

import com.eduai.security.vo.RelationVO;

import java.util.List;

/**
 * 用户关系服务接口
 */
public interface RelationService {

    /** 我的关联列表（已接受）；type 为空返回全部，传 colleague 只返回同事 */
    List<RelationVO> getMyRelations(Long myUid, String type);

    /** 收到的待处理请求；type 为空返回全部 */
    List<RelationVO> getIncomingRelations(Long myUid, String type);

    /** 发送关联请求 */
    RelationVO sendRequest(Long fromUid, Long toUid, String type);

    /** 同意请求（由接收方操作） */
    void approve(Long relationId, Long myUid);

    /** 拒绝请求 */
    void reject(Long relationId, Long myUid);

    /** 移除已有关联（任一方可操作） */
    void removeRelation(Long relationId, Long myUid);
}
