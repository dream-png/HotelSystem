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

import com.hyc.www.dao.inter.UserDao;
import com.hyc.www.po.User;

/**
 * @author <a href="mailto:kobe524348@gmail.com">黄钰朝</a>
 * @program XHotel
 * @description 负责提供User类到数据库的CRUD操作
 * @date 2019-04-09 00:13
 */
public class UserDaoImpl extends BaseDaoImpl implements UserDao {

    String table = "tb_user";
    @Override
    public User getUser(String userName) {
        String sql = "select * from "+table+" where user_name = ?";
        return (User) super.queryList(sql,new Object[]{userName},User.class).get(0);
    }
}
