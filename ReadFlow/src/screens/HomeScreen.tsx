import React, { useState, useEffect } from 'react';
import { View, Text, FlatList, TouchableOpacity, StyleSheet, ActivityIndicator, Alert } from 'react-native';
import { supabase, Document } from '../lib/supabase';
import * as DocumentPicker from 'expo-document-picker';
import * as FileSystem from 'expo-file-system';

export default function HomeScreen({ navigation }: any) {
  const [documents, setDocuments] = useState<Document[]>([]);
  const [loading, setLoading] = useState(true);
  const [uploading, setUploading] = useState(false);

  useEffect(() => {
    fetchDocuments();
  }, []);

  const fetchDocuments = async () => {
    try {
      const { data, error } = await supabase
        .from('documents')
        .select('*')
        .order('created_at', { ascending: false });

      if (error) throw error;
      setDocuments(data || []);
    } catch (error: any) {
      console.error('Error fetching documents:', error);
      Alert.alert('错误', '获取文档列表失败: ' + error.message);
    } finally {
      setLoading(false);
    }
  };

  const uploadDocument = async () => {
    try {
      const result = await DocumentPicker.getDocumentAsync({
        type: ['application/pdf', 'text/plain', 'application/epub+zip'],
        copyToCacheDirectory: true,
      });

      if (result.canceled) return;

      setUploading(true);
      const file = result.assets[0];

      // 上传到 Supabase Storage
      const fileExt = file.name.split('.').pop();
      const fileName = `${Date.now()}.${fileExt}`;
      const filePath = `documents/${fileName}`;

      const { error: uploadError } = await supabase.storage
        .from('documents')
        .upload(filePath, {
          uri: file.uri,
          type: file.mimeType || 'application/octet-stream',
        });

      if (uploadError) throw uploadError;

      // 创建文档记录
      const { error: insertError } = await supabase.from('documents').insert({
        title: file.name,
        file_path: filePath,
        file_type: fileExt || 'unknown',
        processing_status: 'pending',
      });

      if (insertError) throw insertError;

      Alert.alert('成功', '文档上传成功！');
      fetchDocuments();
    } catch (error: any) {
      Alert.alert('错误', '上传失败: ' + error.message);
    } finally {
      setUploading(false);
    }
  };

  const getFileIcon = (type: string) => {
    switch (type) {
      case 'pdf': return '📄';
      case 'epub': return '📖';
      case 'txt': return '📝';
      default: return '📁';
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'completed': return '#4CAF50';
      case 'processing': return '#FF9800';
      case 'failed': return '#F44336';
      default: return '#9E9E9E';
    }
  };

  if (loading) {
    return (
      <View style={styles.centered}>
        <ActivityIndicator size="large" color="#6366F1" />
        <Text style={styles.loadingText}>加载中...</Text>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.title}>📚 我的文档</Text>
        <TouchableOpacity style={styles.uploadBtn} onPress={uploadDocument} disabled={uploading}>
          {uploading ? (
            <ActivityIndicator size="small" color="#FFF" />
          ) : (
            <Text style={styles.uploadBtnText}>+ 上传</Text>
          )}
        </TouchableOpacity>
      </View>

      {documents.length === 0 ? (
        <View style={styles.empty}>
          <Text style={styles.emptyIcon}>📂</Text>
          <Text style={styles.emptyText}>暂无文档</Text>
          <Text style={styles.emptySubtext}>点击上方"上传"添加文档</Text>
        </View>
      ) : (
        <FlatList
          data={documents}
          keyExtractor={(item) => item.id}
          contentContainerStyle={styles.list}
          renderItem={({ item }) => (
            <TouchableOpacity
              style={styles.docCard}
              onPress={() => navigation.navigate('Reader', { document: item })}
            >
              <Text style={styles.docIcon}>{getFileIcon(item.file_type)}</Text>
              <View style={styles.docInfo}>
                <Text style={styles.docTitle} numberOfLines={1}>{item.title}</Text>
                <Text style={styles.docMeta}>
                  {item.page_count_or_virtual_page_count} 页
                </Text>
              </View>
              <View style={[styles.statusBadge, { backgroundColor: getStatusColor(item.processing_status) }]}>
                <Text style={styles.statusText}>
                  {item.processing_status === 'completed' ? '就绪' :
                   item.processing_status === 'processing' ? '处理中' :
                   item.processing_status === 'failed' ? '失败' : '待处理'}
                </Text>
              </View>
            </TouchableOpacity>
          )}
        />
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#F8FAFC' },
  centered: { flex: 1, justifyContent: 'center', alignItems: 'center' },
  loadingText: { marginTop: 10, color: '#64748B', fontSize: 16 },
  header: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', padding: 20, paddingTop: 60, backgroundColor: '#FFF' },
  title: { fontSize: 28, fontWeight: 'bold', color: '#1E293B' },
  uploadBtn: { backgroundColor: '#6366F1', paddingHorizontal: 20, paddingVertical: 10, borderRadius: 25 },
  uploadBtnText: { color: '#FFF', fontSize: 16, fontWeight: '600' },
  empty: { flex: 1, justifyContent: 'center', alignItems: 'center' },
  emptyIcon: { fontSize: 64, marginBottom: 16 },
  emptyText: { fontSize: 20, color: '#64748B', fontWeight: '600' },
  emptySubtext: { fontSize: 14, color: '#94A3B8', marginTop: 8 },
  list: { padding: 16 },
  docCard: { flexDirection: 'row', alignItems: 'center', backgroundColor: '#FFF', padding: 16, borderRadius: 12, marginBottom: 12, elevation: 2, shadowColor: '#000', shadowOffset: { width: 0, height: 2 }, shadowOpacity: 0.1, shadowRadius: 4 },
  docIcon: { fontSize: 40, marginRight: 12 },
  docInfo: { flex: 1 },
  docTitle: { fontSize: 16, fontWeight: '600', color: '#1E293B', marginBottom: 4 },
  docMeta: { fontSize: 14, color: '#64748B' },
  statusBadge: { paddingHorizontal: 10, paddingVertical: 4, borderRadius: 12 },
  statusText: { color: '#FFF', fontSize: 12, fontWeight: '600' },
});
