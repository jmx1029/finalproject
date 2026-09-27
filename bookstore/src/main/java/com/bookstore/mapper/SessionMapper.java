package com.bookstore.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bookstore.entity.Session;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SessionMapper extends BaseMapper<Session> {

    /**
     * 原子：user1Id 增加未读数
     */
    @Update("UPDATE tb_session SET unread_count_user1 = COALESCE(unread_count_user1, 0) + 1 WHERE id = #{sessionId}")
    int incrementUnreadUser1(@Param("sessionId") Long sessionId);

    /**
     * 原子：user2Id 增加未读数
     */
    @Update("UPDATE tb_session SET unread_count_user2 = COALESCE(unread_count_user2, 0) + 1 WHERE id = #{sessionId}")
    int incrementUnreadUser2(@Param("sessionId") Long sessionId);

    /**
     * 原子：user1Id 清零未读数
     */
    @Update("UPDATE tb_session SET unread_count_user1 = 0 WHERE id = #{sessionId}")
    int clearUnreadUser1(@Param("sessionId") Long sessionId);

    /**
     * 原子：user2Id 清零未读数
     */
    @Update("UPDATE tb_session SET unread_count_user2 = 0 WHERE id = #{sessionId}")
    int clearUnreadUser2(@Param("sessionId") Long sessionId);
}
