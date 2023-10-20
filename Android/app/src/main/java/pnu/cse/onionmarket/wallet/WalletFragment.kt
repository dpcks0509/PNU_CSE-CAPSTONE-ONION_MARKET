package pnu.cse.onionmarket.wallet

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import pnu.cse.onionmarket.R
import pnu.cse.onionmarket.databinding.FragmentWalletBinding
import pnu.cse.onionmarket.payment.transaction.TransactionAdapter
import pnu.cse.onionmarket.payment.transaction.TransactionItem

class WalletFragment : Fragment(R.layout.fragment_wallet) {
    private lateinit var binding: FragmentWalletBinding
    private lateinit var walletAdapter: WalletAdapter
    private lateinit var transactionAdapter: TransactionAdapter

    private var transactionList = mutableListOf<TransactionItem>()

    private var walletList = mutableListOf<WalletItem>()
    private var walletExist = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentWalletBinding.bind(view)

        val userId = Firebase.auth.currentUser?.uid

        walletAdapter = WalletAdapter()

        transactionAdapter = TransactionAdapter { item ->
            val action = WalletFragmentDirections.actionWalletFragmentToPostDetailFragment(
                postId = item.postId!!, writerId = item.sellerId!!
            )
            findNavController().navigate(action)
        }

        Firebase.database.reference.child("Wallets")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    walletList = mutableListOf()

                    snapshot.children.map {
                        val wallet = it.getValue(WalletItem::class.java)
                        wallet ?: return
                        if (wallet.userId == userId) {
                            walletList.add(wallet)
                        }

                    }

                    walletList.sortBy { it.createdAt }
                    walletAdapter.submitList(walletList)

                    if (walletAdapter.currentList.isEmpty()) {
                        binding.noWallet.visibility = View.VISIBLE
                        binding.transactionText.visibility = View.INVISIBLE
                        binding.noTransaction.visibility = View.INVISIBLE
                        binding.divideLine2.visibility = View.GONE
                        binding.transactionRecyclerView.visibility = View.INVISIBLE
                    } else {
                        binding.noWallet.visibility = View.GONE
                        binding.transactionText.visibility = View.VISIBLE
                        binding.divideLine2.visibility = View.VISIBLE
                        binding.transactionRecyclerView.visibility = View.VISIBLE
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }

            })

        binding.walletRecyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter = walletAdapter
            setHasFixedSize(true)
            addItemDecoration(DividerItemDecoration(context, LinearLayout.VERTICAL))
        }


        Firebase.database.reference.child("Wallets")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    walletExist = false
                    snapshot.children.forEach {
                        val wallet = it.getValue(WalletItem::class.java)
                        wallet ?: return
                        if (wallet.userId == userId) {
                            walletExist = true
                            return@forEach
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {}

            })

        Firebase.database.reference.child("Transactions")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    transactionList = mutableListOf()

                    snapshot.children.map {
                        val transaction = it.getValue(TransactionItem::class.java)
                        transaction ?: return
                        if (transaction.buyerId == userId || transaction.sellerId == userId) {
                            transactionList.add(transaction)
                        }
                    }

                    transactionList.sortedByDescending { it.createdAt }
                    transactionAdapter.submitList(transactionList)

                    if (!walletAdapter.currentList.isEmpty() && transactionAdapter.currentList.isEmpty()) {
                        binding.noTransaction.visibility = View.VISIBLE
                    } else {
                        binding.noTransaction.visibility = View.INVISIBLE
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })

        binding.transactionRecyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter = transactionAdapter
            setHasFixedSize(true)
            addItemDecoration(DividerItemDecoration(context, LinearLayout.VERTICAL))
        }

        binding.addButton.setOnClickListener {
            if (walletExist) {
                Toast.makeText(context, "지갑은 한개만 등록가능합니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            findNavController().navigate(R.id.action_walletFragment_to_walletAddFragment)
        }
    }
}