package com.bookstore.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bookstore.entity.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {

    /**
     * 将会话中所有消息标记为已读
     */
    @Update("UPDATE tb_message SET is_read = 1, read_time = NOW() " +
            "WHERE session_id = #{sessionId} AND receiver_id = #{userId} AND is_read = 0 AND is_deleted = 0")
    int markAllAsRead(@Param("sessionId") Long sessionId, @Param("userId") Long userId);
}