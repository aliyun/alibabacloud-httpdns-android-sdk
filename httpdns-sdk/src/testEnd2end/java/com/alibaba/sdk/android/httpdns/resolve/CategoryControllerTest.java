package com.alibaba.sdk.android.httpdns.resolve;

import com.alibaba.sdk.android.httpdns.serverip.RegionServerScheduleService;

import org.hamcrest.MatcherAssert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;

/**
 * @author zonglin.nzl
 * @date 2020/12/3
 */
@RunWith(RobolectricTestRunner.class)
public class CategoryControllerTest {

	private RegionServerScheduleService scheduleService = Mockito.mock(
		RegionServerScheduleService.class);
	private CategoryController categoryController = new CategoryController(scheduleService);

	@Test
	public void defaultIsNormalCategory() {
		MatcherAssert.assertThat("default is NormalCategory",
			categoryController.getCategory() instanceof NormalResolveCategory);
	}

	@Test
	public void turnDownTwiceWillChangeToSniffCategory() {
		categoryController.turnDown();
		MatcherAssert.assertThat("turn down once is NormalCategory",
			categoryController.getCategory() instanceof NormalResolveCategory);
		categoryController.turnDown();
		MatcherAssert.assertThat("turn down twice is SniffCategory",
			categoryController.getCategory() instanceof SniffResolveCategory);
		categoryController.turnDown();
		MatcherAssert.assertThat("turn down more than twice will not change",
			categoryController.getCategory() instanceof SniffResolveCategory);
	}

	@Test
	public void turnUpOnceWillChangeToNormalCategory() {
		categoryController.turnDown();
		categoryController.turnDown();
		MatcherAssert.assertThat("ensure now is SniffCategory",
			categoryController.getCategory() instanceof SniffResolveCategory);
		categoryController.turnUp();
		MatcherAssert.assertThat("turn up once is NormalCategory",
			categoryController.getCategory() instanceof NormalResolveCategory);
		categoryController.turnUp();
		MatcherAssert.assertThat("turn up more will not change",
			categoryController.getCategory() instanceof NormalResolveCategory);
	}

	@Test
	public void resetWillChangeToNormalCategory() {
		categoryController.turnDown();
		categoryController.turnDown();
		MatcherAssert.assertThat("ensure now is SniffCategory",
			categoryController.getCategory() instanceof SniffResolveCategory);
		categoryController.reset();
		MatcherAssert.assertThat("reset to NormalCategory",
			categoryController.getCategory() instanceof NormalResolveCategory);
	}
}
