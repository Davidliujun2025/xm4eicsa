package com.example.aics.service;

import com.example.aics.dto.PageResponse;
import com.example.aics.dto.ToggleFavoriteRequest;
import com.example.aics.dto.ToggleFavoriteResponse;
import com.example.aics.exception.LibraryFullException;
import com.example.aics.repository.ScriptFavoriteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@TestPropertySource(properties = "app.script-library.max-size=3")
class ScriptFavoriteServiceTest {

    @Autowired
    private ScriptFavoriteService service;

    @Autowired
    private ScriptFavoriteRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void shouldFavoriteAndCancelSameScript() {
        ToggleFavoriteRequest request = request("talk-1", "退货安抚话术", List.of("退货安抚"));

        ToggleFavoriteResponse favorited = service.toggle("csr-001", request);
        ToggleFavoriteResponse cancelled = service.toggle("csr-001", requestWithoutTags("talk-1", "退货安抚话术"));

        assertThat(favorited.favorited()).isTrue();
        assertThat(cancelled.favorited()).isFalse();
        assertThat(repository.countByStaffId("csr-001")).isZero();
    }

    @Test
    void shouldRequireTagsWhenCreatingFavorite() {
        ToggleFavoriteRequest request = requestWithoutTags("talk-1", "退货安抚话术");

        assertThatThrownBy(() -> service.toggle("csr-001", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("收藏话术时至少选择1个标签");
    }

    @Test
    void shouldSearchByKeywordOnlyInContentOrTags() {
        service.toggle("csr-001", request("talk-1", "这是一段物流延迟安抚话术", List.of("物流")));
        service.toggle("csr-001", request("talk-2", "亲，退货问题我们马上帮您处理", List.of("退货安抚")));
        service.toggle("csr-001", request("talk-3", "尺码推荐话术", List.of("售前")));

        PageResponse<?> result = service.search("csr-001", "退货安抚", null, 0, 20);

        assertThat(result.total()).isEqualTo(1);
    }

    @Test
    void shouldRejectNewFavoriteWhenLibraryIsFull() {
        service.toggle("csr-001", request("talk-1", "第一条话术", List.of("售前")));
        service.toggle("csr-001", request("talk-2", "第二条话术", List.of("物流")));
        service.toggle("csr-001", request("talk-3", "第三条话术", List.of("售后")));

        assertThatThrownBy(() -> service.toggle("csr-001", request("talk-4", "第四条话术", List.of("安抚"))))
                .isInstanceOf(LibraryFullException.class)
                .hasMessage("话术库已达上限，请清理旧话术");
    }

    private ToggleFavoriteRequest request(String sourceTalkId, String content, List<String> tags) {
        return new ToggleFavoriteRequest(sourceTalkId, content, "测试场景", Instant.parse("2026-07-22T10:20:30Z"), tags);
    }

    private ToggleFavoriteRequest requestWithoutTags(String sourceTalkId, String content) {
        return new ToggleFavoriteRequest(sourceTalkId, content, "测试场景", Instant.parse("2026-07-22T10:20:30Z"), null);
    }
}
