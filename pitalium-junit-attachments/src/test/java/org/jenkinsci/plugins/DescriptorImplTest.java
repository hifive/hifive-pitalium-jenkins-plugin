package org.jenkinsci.plugins;

import static org.junit.Assert.assertEquals;

import org.jenkinsci.plugins.PtlPublisher.DescriptorImpl;
import org.junit.Test;

/**
 * Test DescriptorImpl
 * 
 * @author FPT
 *
 */
public class DescriptorImplTest {

	/**
	 * 表示名を確認する。
	 */
	@Test
	public void testGetDisplayName() {
		DescriptorImpl descriptorImpl = new DescriptorImpl();
		// 戻り値が「Pitaliumプラグイン」である。
		assertEquals("Pitaliumプラグイン", descriptorImpl.getDisplayName());
	}
}
