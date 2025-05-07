package com.jeequan.jeepay.core.handler;

import com.alibaba.fastjson.JSON;
import com.jeequan.jeepay.core.entity.RemarkConfig;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.springframework.stereotype.Component;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * description...
 *
 * @author zkye
 * @version 1.0
 * @see <a href=""></a>
 * @since 2025/4/25
 */
 @Component
 public class RemarkConfigTypeHandler extends BaseTypeHandler<RemarkConfig> {

     @Override
     public void setNonNullParameter(PreparedStatement ps, int i, RemarkConfig parameter, JdbcType jdbcType) throws SQLException {
         ps.setString(i, toJsonString(parameter));
     }

     @Override
     public RemarkConfig getNullableResult(ResultSet rs, String columnName) throws SQLException {
         return toObject(rs.getString(columnName));
     }

     @Override
     public RemarkConfig getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
         return toObject(rs.getString(columnIndex));
     }

     @Override
     public RemarkConfig getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
         return toObject(cs.getString(columnIndex));
     }

     private String toJsonString(RemarkConfig obj) {
         return JSON.toJSONString(obj); // 使用 JSON 工具序列化
     }

     private RemarkConfig toObject(String json) {
         return JSON.parseObject(json, RemarkConfig.class); // 使用 JSON 工具反序列化
     }
 }
