import React, { useState } from 'react';
import { View, Text, ScrollView, TouchableOpacity, StyleSheet, Dimensions, ActivityIndicator } from 'react-native';
import { Document } from '../lib/supabase';

const { width, height } = Dimensions.get('window');

export default function ReaderScreen({ route, navigation }: any) {
  const { document } = route.params as { document: Document };
  const [currentPage, setCurrentPage] = useState(1);
  const [totalPages] = useState(document.page_count_or_virtual_page_count || 1);
  const [loading, setLoading] = useState(false);

  const handlePageChange = (page: number) => {
    if (page >= 1 && page <= totalPages) {
      setCurrentPage(page);
    }
  };

  return (
    <View style={styles.container}>
      {/* Header */}
      <View style={styles.header}>
        <TouchableOpacity onPress={() => navigation.goBack()}>
          <Text style={styles.backBtn}>← 返回</Text>
        </TouchableOpacity>
        <Text style={styles.title} numberOfLines={1}>{document.title}</Text>
        <View style={{ width: 60 }} />
      </View>

      {/* Content Area */}
      <ScrollView style={styles.content} contentContainerStyle={styles.contentContainer}>
        {loading ? (
          <View style={styles.loading}>
            <ActivityIndicator size="large" color="#6366F1" />
            <Text style={styles.loadingText}>加载中...</Text>
          </View>
        ) : (
          <View style={styles.pageContainer}>
            {/* PDF/EPUB 渲染区域 */}
            <View style={styles.page}>
              <Text style={styles.placeholderText}>
                📄 {document.title}
              </Text>
              <Text style={styles.pageInfo}>
                第 {currentPage} / {totalPages} 页
              </Text>
              
              {document.processing_status === 'pending' && (
                <View style={styles.statusBanner}>
                  <Text style={styles.statusText}>📤 文档正在后台处理中，请稍候...</Text>
                </View>
              )}
              
              {document.processing_status === 'failed' && (
                <View style={[styles.statusBanner, styles.errorBanner]}>
                  <Text style={[styles.statusText, styles.errorText]}>❌ 文档处理失败</Text>
                </View>
              )}
              
              {document.processing_status === 'completed' && (
                <Text style={styles.readyText}>✅ 文档已准备就绪</Text>
              )}
            </View>
          </View>
        )}
      </ScrollView>

      {/* Bottom Toolbar */}
      <View style={styles.toolbar}>
        <TouchableOpacity style={styles.toolBtn} onPress={() => handlePageChange(currentPage - 1)}>
          <Text style={styles.toolBtnText}>◀ 上一页</Text>
        </TouchableOpacity>

        <View style={styles.pageIndicator}>
          <Text style={styles.pageIndicatorText}>{currentPage} / {totalPages}</Text>
        </View>

        <TouchableOpacity style={styles.toolBtn} onPress={() => handlePageChange(currentPage + 1)}>
          <Text style={styles.toolBtnText}>下一页 ▶</Text>
        </TouchableOpacity>
      </View>

      {/* Quick Actions */}
      <View style={styles.actions}>
        <TouchableOpacity style={styles.actionBtn}>
          <Text style={styles.actionIcon}>🔍</Text>
          <Text style={styles.actionText}>AI 问答</Text>
        </TouchableOpacity>
        <TouchableOpacity style={styles.actionBtn}>
          <Text style={styles.actionIcon}>✏️</Text>
          <Text style={styles.actionText}>笔记</Text>
        </TouchableOpacity>
        <TouchableOpacity style={styles.actionBtn}>
          <Text style={styles.actionIcon}>🖍️</Text>
          <Text style={styles.actionText}>标注</Text>
        </TouchableOpacity>
        <TouchableOpacity style={styles.actionBtn}>
          <Text style={styles.actionIcon}>📑</Text>
          <Text style={styles.actionText}>目录</Text>
        </TouchableOpacity>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#F8FAFC' },
  header: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', padding: 16, paddingTop: 50, backgroundColor: '#FFF', borderBottomWidth: 1, borderBottomColor: '#E2E8F0' },
  backBtn: { fontSize: 16, color: '#6366F1' },
  title: { fontSize: 16, fontWeight: '600', color: '#1E293B', flex: 1, textAlign: 'center', marginHorizontal: 16 },
  content: { flex: 1 },
  contentContainer: { flexGrow: 1 },
  loading: { flex: 1, justifyContent: 'center', alignItems: 'center', padding: 40 },
  loadingText: { marginTop: 16, fontSize: 16, color: '#64748B' },
  pageContainer: { flex: 1, padding: 16 },
  page: { backgroundColor: '#FFF', borderRadius: 12, padding: 24, minHeight: height * 0.5, elevation: 2, justifyContent: 'center', alignItems: 'center' },
  placeholderText: { fontSize: 18, color: '#1E293B', textAlign: 'center', marginBottom: 16 },
  pageInfo: { fontSize: 14, color: '#64748B' },
  statusBanner: { marginTop: 24, backgroundColor: '#FEF3C7', padding: 16, borderRadius: 8 },
  statusText: { color: '#92400E', textAlign: 'center', fontSize: 14 },
  errorBanner: { backgroundColor: '#FEE2E2' },
  errorText: { color: '#991B1B' },
  readyText: { marginTop: 24, color: '#059669', fontSize: 16 },
  toolbar: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', padding: 16, backgroundColor: '#FFF', borderTopWidth: 1, borderTopColor: '#E2E8F0' },
  toolBtn: { paddingVertical: 8, paddingHorizontal: 16 },
  toolBtnText: { fontSize: 14, color: '#6366F1' },
  pageIndicator: { backgroundColor: '#F1F5F9', paddingHorizontal: 16, paddingVertical: 8, borderRadius: 20 },
  pageIndicatorText: { fontSize: 14, color: '#64748B', fontWeight: '600' },
  actions: { flexDirection: 'row', justifyContent: 'space-around', padding: 16, paddingBottom: 32, backgroundColor: '#FFF' },
  actionBtn: { alignItems: 'center', padding: 12 },
  actionIcon: { fontSize: 24, marginBottom: 4 },
  actionText: { fontSize: 12, color: '#64748B' },
});
