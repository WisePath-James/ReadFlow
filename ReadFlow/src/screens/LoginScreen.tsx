import React, { useState } from 'react';
import { View, Text, TextInput, TouchableOpacity, StyleSheet, Alert, KeyboardAvoidingView, Platform, ActivityIndicator } from 'react-native';
import { supabase } from '../lib/supabase';

export default function LoginScreen({ navigation }: any) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);

  const handleLogin = async () => {
    if (!email || !password) {
      Alert.alert('错误', '请输入邮箱和密码');
      return;
    }

    setLoading(true);
    try {
      const { data, error } = await supabase.auth.signInWithPassword({
        email,
        password,
      });

      if (error) throw error;
      
      navigation.replace('Main');
    } catch (error: any) {
      Alert.alert('登录失败', error.message);
    } finally {
      setLoading(false);
    }
  };

  const handleSignUp = async () => {
    if (!email || !password) {
      Alert.alert('错误', '请输入邮箱和密码');
      return;
    }

    setLoading(true);
    try {
      const { data, error } = await supabase.auth.signUp({
        email,
        password,
      });

      if (error) throw error;
      
      Alert.alert('成功', '注册成功！请查收验证邮件。');
    } catch (error: any) {
      Alert.alert('注册失败', error.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <KeyboardAvoidingView style={styles.container} behavior={Platform.OS === 'ios' ? 'padding' : 'height'}>
      <View style={styles.content}>
        <Text style={styles.logo}>📖 ReadFlow</Text>
        <Text style={styles.subtitle}>智能文档阅读与问答助手</Text>

        <View style={styles.form}>
          <TextInput
            style={styles.input}
            placeholder="邮箱"
            value={email}
            onChangeText={setEmail}
            autoCapitalize="none"
            keyboardType="email-address"
            autoCorrect={false}
          />
          <TextInput
            style={styles.input}
            placeholder="密码"
            value={password}
            onChangeText={setPassword}
            secureTextEntry
          />

          <TouchableOpacity style={styles.loginBtn} onPress={handleLogin} disabled={loading}>
            {loading ? (
              <ActivityIndicator size="small" color="#FFF" />
            ) : (
              <Text style={styles.loginBtnText}>登录</Text>
            )}
          </TouchableOpacity>

          <TouchableOpacity style={styles.signupBtn} onPress={handleSignUp} disabled={loading}>
            <Text style={styles.signupBtnText}>没有账号？注册</Text>
          </TouchableOpacity>
        </View>
      </View>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#6366F1' },
  content: { flex: 1, justifyContent: 'center', padding: 24 },
  logo: { fontSize: 48, fontWeight: 'bold', color: '#FFF', textAlign: 'center', marginBottom: 8 },
  subtitle: { fontSize: 16, color: 'rgba(255,255,255,0.8)', textAlign: 'center', marginBottom: 48 },
  form: { backgroundColor: '#FFF', borderRadius: 16, padding: 24 },
  input: { backgroundColor: '#F1F5F9', borderRadius: 12, padding: 16, fontSize: 16, marginBottom: 16 },
  loginBtn: { backgroundColor: '#6366F1', borderRadius: 12, padding: 16, alignItems: 'center', marginTop: 8 },
  loginBtnText: { color: '#FFF', fontSize: 18, fontWeight: '600' },
  signupBtn: { marginTop: 16, alignItems: 'center' },
  signupBtnText: { color: '#6366F1', fontSize: 16 },
});
