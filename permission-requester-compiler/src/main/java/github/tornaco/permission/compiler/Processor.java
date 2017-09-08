package github.tornaco.permission.compiler;

import com.squareup.javapoet.MethodSpec;

/**
 * Created by guohao4 on 2017/9/6.
 * Email: Tornaco@163.com
 */

public interface Processor {
    void processSpec(MethodSpec.Builder builder);
}
