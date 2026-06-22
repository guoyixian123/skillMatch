package com.skillmatch.domain.query;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 通知列表查询参数，继承分页参数（page + size）
 * 目前无额外筛选条件，预留扩展
 *
 * @author Speed
 * @since 2026-05-27
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class NotificationQuery extends PageQuery {
    /** 通知类型筛选：1=主页赞 2=帖子赞 3=评论，不传则查全部 */
    private Integer type;
}
