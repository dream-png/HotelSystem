/*
 * Copyright (c) 2019.  黄钰朝
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hyc.www.dao.impl;

import com.hyc.www.dao.inter.BaseDao;
import com.hyc.www.dao.inter.ResultMapper;
import com.hyc.www.exception.DaoException;
import com.hyc.www.util.JdbcUtils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.*;
import java.util.LinkedList;

/**
 * @author <a href="mailto:kobe524348@gmail.com">黄钰朝</a>
 * @program XHotel
 * @description 所有Dao接口实现类的父类，提供共有数据库操作
 * @date 2019-04-09 00:26
 */
public class BaseDaoImpl implements BaseDao {


    /**
     * 执行一个预编译更新语句
     *
     * @param sql    sql语句
     * @param params 参数
     * @return int 执行sql语句后影响记录的行数
     * @name executeUpdate
     * @notice sql语句中的表名不能在预编译中设置，sql语句必须已经填好表名，只有字段可以使用占位符<br>
     * 请不要使用以下这种sql语句：<br>
     * executeUpdate("insert into ? (user_name) values (?)",new Object[]{"tb_user","testdao"});<br>
     * 这种语句设置参数后是下面这样：<br>
     * insert into 'tb_user' (user_name) values ('testdao')<br>
     * 表名带上了引号，这种语句显然无法正常执行<br>
     * @author <a href="mailto:kobe524348@gmail.com">黄钰朝</a>
     * @date 2019/4/9
     */
    @Override
    public int executeUpdate(String sql, Object[] params) {

        /**
         * 请不要对Connection使用auto-close，应该使用JdbcUtils提供的close方法放回数据库连接池
         */
        Connection conn = JdbcUtils.getConnection();
        int result = 0;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            /**
             * 给预编译语句填入参数
             */
            JdbcUtils.setParams(ps, params);
            sql = ps.toString();
            result = ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
            throw new DaoException("执行预编译更新语句异常：" + sql, e);
        } finally {
            JdbcUtils.close(conn);
        }
        return result;
    }


    /**
     * @param sql    要执行的预编译sql语句
     * @param params 参数
     * @param mapper 结果集映射器，需要实现ResultMapper接口的doMap方法
     * @return java.lang.Object
     * @name executeQuery
     * @notice none
     * @author <a href="mailto:kobe524348@gmail.com">黄钰朝</a>
     * @date 2019/4/9
     */
    @Override
    public Object executeQuery(String sql, Object[] params, ResultMapper mapper) {

        Connection conn = JdbcUtils.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            JdbcUtils.setParams(ps, params);
            ResultSet rs = ps.executeQuery();
            /**
             * 调用接口中的方法映射结果集，使用时该接口必须有实现类
             */
            return mapper.doMap(rs);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new DaoException("执行预编译查询语句产生异常：" + sql, e);
        } finally {
            JdbcUtils.close(conn);
        }
    }

    /**
     * @param table 要插入的表名
     * @param obj   要插入的对象
     * @return int 更新的数据库记录数
     * @name insert
     * @notice none
     * @author <a href="mailto:kobe524348@gmail.com">黄钰朝</a>
     * @date 2019/4/9
     */
    @Override
    public int insert(String table, Object obj) {
        LinkedList<String> namelist = new LinkedList<>();
        LinkedList<Object> valuelist = new LinkedList<>();
        fieldMapper(obj, namelist, valuelist);
        /**
         * 根据属性名和属性值生成sql语句
         */
        StringBuilder sql = new StringBuilder("insert into " + table + " (");
        for (Object name : namelist.toArray()) {
            sql.append(name.toString() + ",");
        }
        sql.setCharAt(sql.length() - 1, ')');
        sql.append(" values (");
        for (int i = 0; i < namelist.size(); i++) {
            sql.append("?,");
        }
        sql.setCharAt(sql.length() - 1, ')');
        return executeUpdate(sql.toString(), valuelist.toArray());
    }


    /**
     * 根据传入的表名和id，从该表中删除该条记录
     *
     * @param table 要删除记录所在的表名
     * @param id    要删除的记录的id
     * @return int 更新的数据库记录数
     * @name delete
     * @notice none
     * @author <a href="mailto:kobe524348@gmail.com">黄钰朝</a>
     * @date 2019/4/10
     */
    @Override
    public int delete(String table, Object id) {
        String sql = "delete from " + table + " where id = ?";
        return executeUpdate(sql, new Object[]{id});
    }


    /**
     * 根据传入的表名和id，从该表中更新一条记录
     *
     * @param obj 要更新的记录对应的实体类对象
     * @return int 更新的数据库记录数
     * @name update
     * @notice 请注意此方法默认更新该对象中所有不为null的属性到数据库<br>
     * 如果不希望更新某些字段，请将其设置为null
     * @author <a href="mailto:kobe524348@gmail.com">黄钰朝</a>
     * @date 2019/4/10
     */
    @Override
    public int update(String table, Object obj) {
        LinkedList<String> namelist = new LinkedList<>();
        LinkedList<Object> valuelist = new LinkedList<>();
        fieldMapper(obj, namelist, valuelist);
        Object[] params = valuelist.toArray();
        /**
         * 根据对象的属性名和属性值生成sql语句
         */
        StringBuilder sql = new StringBuilder("update " + table + " set ");
        for (Object name : namelist.toArray()) {
            sql.append(name + " = ?,");
        }
        sql.setCharAt(sql.length() - 1, ' ');
        try {
            sql.append(" where id = " + obj.getClass().getDeclaredMethod("getId").invoke(obj));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            throw new DaoException("反射执行getId方法异常：找不到getId方法", e);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            throw new DaoException("反射执行getId方法异常：无法执行getId方法", e);
        }
        return executeUpdate(sql.toString(), params);
    }

    /**
     * 执行输入的sql语句，并且将结果以LinkedList的形式返回
     *
     * @param sql    查询的预编译sql语句
     * @param params 预编译的参数
     * @param clazz  用于映射结果集的实体类
     * @return java.util.LinkedList
     * @name queryList
     * @notice none
     * @author <a href="mailto:kobe524348@gmail.com">黄钰朝</a>
     * @date 2019/4/10
     */
    @Override
    public LinkedList<Object> queryList(String sql, Object[] params, Class clazz) {
        return (LinkedList<Object>) executeQuery(sql, params, new ListMapper(clazz));
    }


    /**
     * @param obj       需要被映射的对象
     * @param nameList  将映射的属性名返回在这个集合中
     * @param valueList 将映射的属性值返回在这个集合中
     * @name fieldMapper
     * @notice none
     * @author <a href="mailto:kobe524348@gmail.com">黄钰朝</a>
     * @date 2019/4/9
     */
    private void fieldMapper(Object obj, LinkedList<String> nameList, LinkedList<Object> valueList) {
        for (Field field : obj.getClass().getDeclaredFields()) {
            /**
             * 取出每个属性的值
             */
            for (Method method : obj.getClass().getDeclaredMethods()) {
                if (method.getName().startsWith("get") && method.getName().substring(3).equalsIgnoreCase(field.getName())) {
                    Object fieldVaule = null;
                    try {
                        fieldVaule = method.invoke(obj);
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw new DaoException("反射执行get方法异常：" + method.getName(), e);
                    }
                    /**
                     * 只添加不为null值的字段
                     */
                    if (fieldVaule != null) {
                        valueList.add(fieldVaule);
                        /**
                         * 取出该属性的名称，映射成数据库字段名
                         */
                        byte[] bytes = field.getName().getBytes();
                        StringBuilder fieldName = new StringBuilder();
                        for (int i = 0; i < bytes.length; i++) {
                            if (bytes[i] > 'A' && bytes[i] < 'Z') {
                                fieldName.append('_');
                            }
                            fieldName.append((char) bytes[i]);
                        }
                        nameList.add(fieldName.toString());
                    }
                }
            }
        }
    }


    /**
     * ResultMapper的一个实现类，提供将结果集映射为一个List的方法<br>
     * 此类的对象在使用前必须设置一个用于映射结果的实体类
     *
     * @author <a href="mailto:kobe524348@gmail.com">黄钰朝</a>
     * @name ListMapper
     * @notice none
     * @date 2019/4/10
     */
    private class ListMapper implements ResultMapper {
        Class clazz = null;

        @Override
        public Object doMap(ResultSet rs) {
            LinkedList<Object> list = new LinkedList<>();

            try {
                ResultSetMetaData md = rs.getMetaData();
                Method[] method = clazz.getDeclaredMethods();
                int colCount = md.getColumnCount();
                String[] setters = new String[colCount + 1];
                String[] columns = new String[colCount + 1];
                for (int i = 0; i < colCount; i++) {
                    /**
                     * 取得字段名,存在columns数组中
                     */
                    columns[i] = md.getColumnLabel(i + 1);
                    /**
                     * 取得字段名并映射为setter方法名，忽略大小写,存在setters数组中
                     */
                    String[] split = md.getColumnLabel(i + 1).split("_");
                    StringBuilder setter = new StringBuilder("set");
                    for (int j = 0; j < split.length; j++) {
                        setter.append(split[j]);
                    }
                    setters[i] = setter.toString();
                }

                while (rs.next()) {
                    Object obj = clazz.newInstance();
                    for (int i = 0; i < colCount; i++) {
                        /**
                         * 执行对应的set方法，忽略大小写
                         */
                        for (Method ms : method) {
                            if (ms.getName().equalsIgnoreCase(setters[i])) {
                                ms.invoke(obj, rs.getObject(columns[i]));
                            }
                        }
                    }
                    list.add(obj);
                }
            } catch (SQLException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
                e.printStackTrace();
                throw new DaoException("映射结果集产生异常：" + e.getMessage(), e);
            }
            return list;
        }

        public ListMapper(Class clazz) {
            this.clazz = clazz;
        }

        public ListMapper() {
        }

        public Class getClazz() {
            return clazz;
        }

        public void setClazz(Class clazz) {
            this.clazz = clazz;
        }
    }


}

